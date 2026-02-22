const API_BASE = "/api";
const TOKEN_STORAGE_KEY = "lunaroj.auth.token";
const TOKEN_EXPIRE_SKEW_MS = 5 * 1000;

export const AUTH_TOKEN_CHANGED_EVENT = "lunaroj:auth-token-changed";
export const AUTH_TOKEN_REFRESHED_EVENT = "lunaroj:auth-token-refreshed";

// 调试开关：true 时每次 refresh 成功会触发前端提示；false 为完全静默续期。
export const AUTH_REFRESH_NOTICE_CONFIG = {
  enabled: true
};

let refreshAccessPromise = null;

function createAuthExpiredError() {
  return new Error("登录状态已过期，请重新登录。");
}

function emitAuthTokenChanged() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(AUTH_TOKEN_CHANGED_EVENT));
  }
}

function emitAuthTokenRefreshed(message) {
  if (!AUTH_REFRESH_NOTICE_CONFIG.enabled || typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(
    new CustomEvent(AUTH_TOKEN_REFRESHED_EVENT, {
      detail: {
        message
      }
    })
  );
}

function decodeBase64Url(payload) {
  try {
    const normalized = String(payload).replace(/-/g, "+").replace(/_/g, "/");
    const padLength = (4 - (normalized.length % 4)) % 4;
    return atob(`${normalized}${"=".repeat(padLength)}`);
  } catch {
    return "";
  }
}

function parseJwtPayload(token) {
  if (typeof token !== "string" || !token) {
    return null;
  }
  const parts = token.split(".");
  if (parts.length < 2) {
    return null;
  }
  const rawPayload = decodeBase64Url(parts[1]);
  if (!rawPayload) {
    return null;
  }
  try {
    return JSON.parse(rawPayload);
  } catch {
    return null;
  }
}

function resolveAccessTokenExpireAt(tokenPayload) {
  if (!tokenPayload) {
    return 0;
  }
  if (typeof tokenPayload.issuedAt === "number" && typeof tokenPayload.expiresIn === "number") {
    return tokenPayload.issuedAt + tokenPayload.expiresIn * 1000;
  }
  const exp = parseJwtPayload(tokenPayload.accessToken)?.exp;
  if (typeof exp === "number" && Number.isFinite(exp)) {
    return exp * 1000;
  }
  return 0;
}

function resolveRefreshTokenExpireAt(tokenPayload) {
  if (!tokenPayload) {
    return 0;
  }
  const exp = parseJwtPayload(tokenPayload.refreshToken)?.exp;
  if (typeof exp === "number" && Number.isFinite(exp)) {
    return exp * 1000;
  }
  return 0;
}

export function isAuthTokenExpired(tokenPayload) {
  const expireAt = resolveAccessTokenExpireAt(tokenPayload);
  if (!expireAt) {
    return false;
  }
  return Date.now() >= expireAt - TOKEN_EXPIRE_SKEW_MS;
}

export function isRefreshTokenExpired(tokenPayload) {
  const expireAt = resolveRefreshTokenExpireAt(tokenPayload);
  if (!expireAt) {
    return false;
  }
  return Date.now() >= expireAt - TOKEN_EXPIRE_SKEW_MS;
}

export function getAuthTokenExpireAt(tokenPayload) {
  return resolveAccessTokenExpireAt(tokenPayload);
}

export function getRefreshTokenExpireAt(tokenPayload) {
  return resolveRefreshTokenExpireAt(tokenPayload);
}

function readTokenFromStorage(storage, storageType) {
  const raw = storage.getItem(TOKEN_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return {
      ...JSON.parse(raw),
      _storageType: storageType
    };
  } catch {
    storage.removeItem(TOKEN_STORAGE_KEY);
    return null;
  }
}

export function clearAuthToken(emitEvent = true) {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  if (emitEvent) {
    emitAuthTokenChanged();
  }
}

export function saveAuthToken(tokenPayload, keepLogin = true) {
  clearAuthToken(false);
  const json = JSON.stringify(tokenPayload);
  if (keepLogin) {
    localStorage.setItem(TOKEN_STORAGE_KEY, json);
  } else {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, json);
  }
  emitAuthTokenChanged();
}

export function getAuthToken() {
  const token =
    readTokenFromStorage(localStorage, "local") || readTokenFromStorage(sessionStorage, "session");
  if (!token) {
    return null;
  }
  if (isRefreshTokenExpired(token)) {
    clearAuthToken();
    return null;
  }
  return token;
}

async function sendHttpRequest(path, options, accessToken = "") {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  if (accessToken) {
    headers.Authorization = `${options.tokenType || "Bearer"} ${accessToken}`;
  }

  try {
    return await fetch(`${API_BASE}${path}`, {
      method: options.method || "GET",
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined
    });
  } catch {
    throw new Error("无法与服务器建立连接，请确认服务是否已启动。");
  }
}

async function parseApiResponse(response) {
  const responseText = await response.text();
  let payload = null;

  if (!responseText) {
    if (!response.ok) {
      throw new Error(`服务器发生异常（HTTP ${response.status}）。`);
    }
    throw new Error("服务器返回内容为空，请稍后重试。");
  }

  try {
    payload = JSON.parse(responseText);
  } catch {
    if (!response.ok) {
      throw new Error(`服务器发生异常（HTTP ${response.status}）。`);
    }
    throw new Error("服务器响应格式异常，请稍后重试。");
  }

  if (!response.ok) {
    throw new Error(payload?.message || "请求失败。");
  }
  if (!payload || payload.code !== 0) {
    throw new Error(payload?.message || "业务请求失败。");
  }
  return payload.data;
}

async function refreshAccessToken() {
  if (refreshAccessPromise) {
    return refreshAccessPromise;
  }

  refreshAccessPromise = (async () => {
    const currentToken = getAuthToken();
    if (!currentToken?.refreshToken || isRefreshTokenExpired(currentToken)) {
      clearAuthToken();
      throw createAuthExpiredError();
    }

    const keepLogin = currentToken._storageType !== "session";
    const { _storageType, ...rest } = currentToken;
    const refreshed = await request("/auth/refresh", {
      method: "POST",
      body: {
        refreshToken: currentToken.refreshToken
      }
    });

    const nextTokenPayload = {
      ...rest,
      ...refreshed,
      issuedAt: Date.now()
    };
    saveAuthToken(nextTokenPayload, keepLogin);
    emitAuthTokenRefreshed("续期 access/refresh token 成功");
    return getAuthToken();
  })().finally(() => {
    refreshAccessPromise = null;
  });

  return refreshAccessPromise;
}

async function ensureValidAccessToken() {
  const token = getAuthToken();
  if (!token?.accessToken) {
    throw createAuthExpiredError();
  }
  if (!isAuthTokenExpired(token)) {
    return token;
  }
  try {
    const refreshed = await refreshAccessToken();
    if (!refreshed?.accessToken) {
      throw createAuthExpiredError();
    }
    return refreshed;
  } catch {
    clearAuthToken();
    throw createAuthExpiredError();
  }
}

async function request(path, options = {}) {
  const requiresAuth = Boolean(options.auth);
  let tokenInfo = null;

  if (requiresAuth) {
    tokenInfo = await ensureValidAccessToken();
  }

  let response = await sendHttpRequest(path, options, requiresAuth ? tokenInfo.accessToken : "");

  if (requiresAuth && response.status === 401) {
    try {
      tokenInfo = await refreshAccessToken();
      response = await sendHttpRequest(path, options, tokenInfo?.accessToken || "");
    } catch {
      clearAuthToken();
      throw createAuthExpiredError();
    }
  }

  return parseApiResponse(response);
}

export async function fetchCaptcha() {
  return request("/auth/captcha");
}

export async function fetchRegisterEnabled() {
  return request("/auth/register-enabled");
}

export async function registerUser(form) {
  return request("/auth/register", {
    method: "POST",
    body: form
  });
}

export async function loginUser(form) {
  return request("/auth/login", {
    method: "POST",
    body: form
  });
}

export async function logoutUser() {
  const token = getAuthToken();
  const response = await sendHttpRequest(
    "/auth/logout",
    {
      method: "POST",
      body: {
        refreshToken: token?.refreshToken || null
      },
      tokenType: token?.tokenType || "Bearer"
    },
    token?.accessToken || ""
  );
  return parseApiResponse(response);
}

export async function fetchCurrentUserProfile() {
  return request("/users/me", {
    auth: true
  });
}

export async function fetchUserPublicProfile(username) {
  const target = String(username || "").trim();
  if (!target) {
    throw new Error("用户名不能为空。");
  }
  return request(`/users/${encodeURIComponent(target)}/profile`);
}

export async function updateCurrentUserBasic(form) {
  return request("/users/me/basic", {
    method: "PUT",
    body: form,
    auth: true
  });
}

export async function updateCurrentUserProfile(form) {
  return request("/users/me/profile", {
    method: "PUT",
    body: form,
    auth: true
  });
}

export async function updateCurrentUserPassword(form) {
  return request("/users/me/password", {
    method: "PUT",
    body: form,
    auth: true
  });
}
