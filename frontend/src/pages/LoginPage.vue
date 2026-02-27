<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  clearAuthToken,
  fetchCaptcha,
  fetchCaptchaExpireSeconds,
  fetchRegisterEnabled,
  getAuthToken,
  loginUser,
  saveAuthToken
} from "../api";
import logoUrl from "../assets/LunarOJ-icon.svg";

const DEFAULT_CAPTCHA_EXPIRE_MS = 5 * 60 * 1000;
const router = useRouter();
const route = useRoute();

const registerEnabled = ref(true);
const feedback = reactive({
  type: "",
  text: ""
});
const captchaExpireMs = ref(DEFAULT_CAPTCHA_EXPIRE_MS);
const nowTs = ref(Date.now());
let captchaTimerId = null;

const loading = reactive({
  login: false,
  captchaLogin: false
});

const loginForm = reactive({
  username: "",
  password: "",
  captchaCode: "",
  keepLogin: true
});

const captcha = reactive({
  captchaId: "",
  imageBase64: "",
  expiresAt: 0
});

const tokenInfo = ref(getAuthToken());
const captchaRemainText = computed(() => {
  if (!captcha.captchaId || !captcha.expiresAt) {
    return "";
  }
  const remainSeconds = Math.max(Math.ceil((captcha.expiresAt - nowTs.value) / 1000), 0);
  const minutes = Math.floor(remainSeconds / 60);
  const seconds = remainSeconds % 60;
  return `${minutes}分${String(seconds).padStart(2, "0")}秒`;
});

const tokenPreview = computed(() => {
  if (!tokenInfo.value?.accessToken) {
    return "";
  }
  const token = tokenInfo.value.accessToken;
  if (token.length <= 24) {
    return token;
  }
  return `${token.slice(0, 12)}...${token.slice(-12)}`;
});

const tokenStorageLabel = computed(() => {
  if (!tokenInfo.value) {
    return "";
  }
  return tokenInfo.value._storageType === "session"
    ? "当前保存方式：会话存储（关闭浏览器后失效）"
    : "当前保存方式：本地存储";
});

function setFeedback(type, text) {
  feedback.type = type;
  feedback.text = text;
}

function normalizeCaptchaImageSrc(raw) {
  if (!raw) {
    return "";
  }
  const cleaned = String(raw).trim().replace(/\s+/g, "");
  if (!cleaned) {
    return "";
  }
  const dataPrefix = /^data:image\/[a-zA-Z0-9.+-]+;base64,/i;
  let payload = cleaned;
  while (dataPrefix.test(payload)) {
    payload = payload.replace(dataPrefix, "");
  }
  if (payload.startsWith("base64,")) {
    payload = payload.slice("base64,".length);
  }
  if (!payload) {
    return "";
  }
  const base64Safe = payload.replace(/[^A-Za-z0-9+/=]/g, "");
  return base64Safe ? `data:image/png;base64,${base64Safe}` : "";
}

function isCaptchaExpired() {
  if (!captcha.captchaId || !captcha.expiresAt) {
    return false;
  }
  return nowTs.value >= captcha.expiresAt;
}

async function loadRegisterEnabled() {
  try {
    registerEnabled.value = Boolean(await fetchRegisterEnabled());
  } catch {
    registerEnabled.value = true;
  }
}

async function loadCaptcha() {
  loading.captchaLogin = true;
  try {
    const data = await fetchCaptcha();
    captcha.captchaId = data.captchaId;
    captcha.imageBase64 = normalizeCaptchaImageSrc(data.imageBase64);
    captcha.expiresAt = Date.now() + captchaExpireMs.value;
    loginForm.captchaCode = "";
  } catch (error) {
    setFeedback("error", error.message || "获取验证码失败，请重试。");
  } finally {
    loading.captchaLogin = false;
  }
}

async function loadCaptchaExpireMs() {
  try {
    const expireSeconds = Number(await fetchCaptchaExpireSeconds());
    if (Number.isFinite(expireSeconds) && expireSeconds > 0) {
      captchaExpireMs.value = Math.round(expireSeconds * 1000);
    }
  } catch {
    captchaExpireMs.value = DEFAULT_CAPTCHA_EXPIRE_MS;
  }
}

function handleCaptchaImageError() {
  captcha.imageBase64 = "";
  setFeedback("error", "验证码图片加载失败，请点击刷新验证码。");
}

function validateLoginForm() {
  if (!loginForm.username.trim()) {
    return "请输入用户名。";
  }
  if (!loginForm.password.trim()) {
    return "请输入密码。";
  }
  if (!loginForm.captchaCode.trim()) {
    return "请输入验证码。";
  }
  if (!captcha.captchaId) {
    return "验证码未就绪，请刷新后重试。";
  }
  if (isCaptchaExpired()) {
    return "验证码已过期，请点击刷新验证码。";
  }
  return "";
}

async function handleLogin() {
  const validationError = validateLoginForm();
  if (validationError) {
    setFeedback("error", validationError);
    return;
  }

  loading.login = true;
  let shouldRedirect = false;
  setFeedback("", "");

  try {
    const result = await loginUser({
      username: loginForm.username.trim(),
      password: loginForm.password,
      captchaId: captcha.captchaId,
      captchaCode: loginForm.captchaCode.trim()
    });

    const tokenPayload = {
      ...result,
      issuedAt: Date.now()
    };
    saveAuthToken(tokenPayload, loginForm.keepLogin);
    tokenInfo.value = getAuthToken();
    shouldRedirect = true;
    await router.push("/");
  } catch (error) {
    setFeedback("error", error.message || "登录失败，请重试。");
  } finally {
    loading.login = false;
    if (!shouldRedirect) {
      await loadCaptcha();
    }
  }
}

function clearToken() {
  clearAuthToken();
  tokenInfo.value = null;
  setFeedback("success", "登录信息已清除。");
}

function handleForgotPassword() {
  setFeedback("error", "暂未开放找回密码，请联系管理员处理。");
}

onMounted(async () => {
  captchaTimerId = setInterval(() => {
    nowTs.value = Date.now();
  }, 1000);
  await Promise.all([loadRegisterEnabled(), loadCaptchaExpireMs()]);
  await loadCaptcha();

  const fromRegister = Array.isArray(route.query.fromRegister)
    ? route.query.fromRegister[0]
    : route.query.fromRegister;
  if (fromRegister === "1") {
    const queryUsername = Array.isArray(route.query.username)
      ? route.query.username[0]
      : route.query.username;
    if (typeof queryUsername === "string" && queryUsername.trim() && !loginForm.username.trim()) {
      loginForm.username = queryUsername.trim();
    }
    setFeedback("success", "注册成功，请登录。");
  }
});

onUnmounted(() => {
  if (captchaTimerId) {
    clearInterval(captchaTimerId);
  }
});

function goLogin() {
  router.push("/login");
}

function goRegister() {
  if (!registerEnabled.value) {
    return;
  }
  router.push("/register");
}
</script>

<template>
  <div class="page-bg">
    <main class="auth-shell">
      <section class="card-panel">
        <div class="panel-head">
          <div class="brand-row">
            <img :src="logoUrl" alt="LunarOJ Logo" class="brand-logo" />
            <p class="eyebrow">LunarOJ</p>
          </div>
        </div>

        <div class="tabs" :class="{ single: !registerEnabled }">
          <button class="active" type="button" @click="goLogin">登录</button>
          <button v-if="registerEnabled" type="button" @click="goRegister">注册</button>
        </div>

        <p v-if="feedback.text" class="feedback" :class="feedback.type">
          {{ feedback.text }}
        </p>

        <form class="form" @submit.prevent="handleLogin">
          <label>
            用户名
            <input v-model="loginForm.username" autocomplete="username" maxlength="64" />
          </label>

          <label>
            密码
            <input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              maxlength="64"
            />
          </label>

          <label>
            验证码
            <input
              v-model="loginForm.captchaCode"
              autocomplete="off"
              maxlength="8"
              placeholder="请输入 4 位验证码"
            />
          </label>

          <div class="captcha-block">
            <img
              v-if="captcha.imageBase64"
              :src="captcha.imageBase64"
              alt="登录验证码"
              class="captcha-image"
              @error="handleCaptchaImageError"
            />
            <div v-else class="captcha-image captcha-placeholder">验证码加载失败</div>
            <button
              class="refresh-btn"
              type="button"
              :disabled="loading.captchaLogin"
              @click="loadCaptcha"
            >
              {{ loading.captchaLogin ? "刷新中..." : "刷新验证码" }}
            </button>
          </div>

          <p v-if="captcha.captchaId && !isCaptchaExpired()" class="captcha-countdown-tip">
            验证码将在 {{ captchaRemainText }} 后失效，请及时提交
          </p>

          <button
            v-if="isCaptchaExpired()"
            class="captcha-expired-tip"
            type="button"
            @click="loadCaptcha"
          >
            验证码已过期，点击刷新验证码
          </button>

          <div class="remember-row">
            <span>保持登录状态</span>
            <div class="remember-buttons">
              <button
                type="button"
                :class="{ active: loginForm.keepLogin }"
                @click="loginForm.keepLogin = true"
              >
                是
              </button>
              <button
                type="button"
                :class="{ active: !loginForm.keepLogin }"
                @click="loginForm.keepLogin = false"
              >
                否
              </button>
            </div>
          </div>

          <button class="forgot-btn" type="button" @click="handleForgotPassword">
            忘记密码
          </button>

          <button class="submit-btn" type="submit" :disabled="loading.login">
            {{ loading.login ? "登录中..." : "登录" }}
          </button>
        </form>

        <div v-if="tokenInfo" class="token-box">
          <p class="token-label">当前 Access Token（预览）</p>
          <p class="token-storage">{{ tokenStorageLabel }}</p>
          <code>{{ tokenPreview }}</code>
          <button class="refresh-btn danger" type="button" @click="clearToken">
            清除登录信息
          </button>
        </div>
      </section>
    </main>
  </div>
</template>
