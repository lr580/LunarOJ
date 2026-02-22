<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  AUTH_TOKEN_REFRESHED_EVENT,
  AUTH_TOKEN_CHANGED_EVENT,
  clearAuthToken,
  getAuthToken,
  logoutUser
} from "./api";
import logoUrl from "./assets/LunarOJ-icon.svg";

const router = useRouter();
const route = useRoute();
const tokenInfo = ref(null);
const menuOpen = ref(false);
const menuRef = ref(null);
const refreshToastText = ref("");
const showRefreshToast = ref(false);
let authCheckIntervalId = null;
let refreshToastTimerId = null;

function decodeBase64Url(payload) {
  try {
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
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

function syncAuthToken() {
  tokenInfo.value = getAuthToken();
}

function isProtectedRoute(path) {
  return path === "/profile" || path === "/settings" || path === "/admin";
}

function ensureLogoutIfRequired() {
  if (!isProtectedRoute(route.path)) {
    return;
  }
  closeMenu();
  if (route.path !== "/login") {
    router.push("/login");
  }
}

function checkTokenState() {
  const hadToken = Boolean(tokenInfo.value);
  tokenInfo.value = getAuthToken();
  if (hadToken && !tokenInfo.value) {
    ensureLogoutIfRequired();
  }
}

function handleTokenRefreshed(event) {
  refreshToastText.value = event?.detail?.message || "续期 access/refresh token 成功";
  showRefreshToast.value = true;
  if (refreshToastTimerId) {
    clearTimeout(refreshToastTimerId);
  }
  refreshToastTimerId = setTimeout(() => {
    showRefreshToast.value = false;
  }, 3000);
}

const jwtPayload = computed(() => parseJwtPayload(tokenInfo.value?.accessToken));

const jwtUsername = computed(() => {
  const username = jwtPayload.value?.username;
  return typeof username === "string" ? username.trim() : "";
});

const navDisplayName = computed(() => {
  const tokenPayload = tokenInfo.value;
  if (!tokenPayload) {
    return "";
  }
  if (typeof tokenPayload.nickname === "string" && tokenPayload.nickname.trim()) {
    return tokenPayload.nickname.trim();
  }
  if (typeof tokenPayload.username === "string" && tokenPayload.username.trim()) {
    return tokenPayload.username.trim();
  }
  return jwtUsername.value || "已登录用户";
});

const isAdmin = computed(() => {
  if (!tokenInfo.value) {
    return false;
  }

  const payload = jwtPayload.value || {};
  const roleCandidates = [];

  if (Array.isArray(payload.roles)) {
    roleCandidates.push(...payload.roles);
  }
  if (Array.isArray(payload.authorities)) {
    roleCandidates.push(...payload.authorities);
  }
  if (typeof payload.role === "string") {
    roleCandidates.push(payload.role);
  }
  if (typeof payload.permissionGroup === "string") {
    roleCandidates.push(payload.permissionGroup);
  }
  if (typeof payload.permissionGroupName === "string") {
    roleCandidates.push(payload.permissionGroupName);
  }

  const normalized = roleCandidates
    .map((item) => String(item || "").trim().toUpperCase())
    .filter(Boolean);

  return normalized.some((item) => item.includes("ADMIN") || item.includes("ROOT"));
});

function closeMenu() {
  menuOpen.value = false;
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value;
}

function goTo(path) {
  closeMenu();
  router.push(path);
}

async function handleLogout() {
  closeMenu();
  try {
    await logoutUser();
  } catch {
    // Logout is best-effort; clear local auth state regardless.
  } finally {
    clearAuthToken();
    router.push("/login");
  }
}

function handleDocumentPointerDown(event) {
  if (!menuOpen.value || !menuRef.value) {
    return;
  }
  if (!menuRef.value.contains(event.target)) {
    closeMenu();
  }
}

function handleKeyDown(event) {
  if (event.key === "Escape") {
    closeMenu();
  }
}

onMounted(() => {
  syncAuthToken();
  authCheckIntervalId = setInterval(checkTokenState, 10000);
  window.addEventListener(AUTH_TOKEN_CHANGED_EVENT, syncAuthToken);
  window.addEventListener(AUTH_TOKEN_REFRESHED_EVENT, handleTokenRefreshed);
  document.addEventListener("pointerdown", handleDocumentPointerDown);
  window.addEventListener("keydown", handleKeyDown);
});

onUnmounted(() => {
  if (authCheckIntervalId) {
    clearInterval(authCheckIntervalId);
  }
  if (refreshToastTimerId) {
    clearTimeout(refreshToastTimerId);
  }
  window.removeEventListener(AUTH_TOKEN_CHANGED_EVENT, syncAuthToken);
  window.removeEventListener(AUTH_TOKEN_REFRESHED_EVENT, handleTokenRefreshed);
  document.removeEventListener("pointerdown", handleDocumentPointerDown);
  window.removeEventListener("keydown", handleKeyDown);
});

watch(
  () => route.fullPath,
  () => {
    closeMenu();
    checkTokenState();
  }
);
</script>

<template>
  <div class="app-layout">
    <header class="top-nav">
      <div class="top-nav-left">
        <router-link class="top-brand" to="/">
          <img :src="logoUrl" alt="LunarOJ Logo" class="top-brand-logo" />
          <span class="top-brand-text">LunarOJ</span>
        </router-link>
      </div>
      <div class="top-nav-right">
        <router-link v-if="!tokenInfo" class="top-auth-btn" to="/login">登录</router-link>
        <div v-else ref="menuRef" class="top-user-menu">
          <button
            class="top-auth-name top-auth-trigger"
            type="button"
            :aria-expanded="menuOpen ? 'true' : 'false'"
            aria-haspopup="menu"
            @click.stop="toggleMenu"
          >
            <span class="top-auth-name-text">{{ navDisplayName }}</span>
            <svg class="top-auth-arrow" viewBox="0 0 16 16" aria-hidden="true">
              <path d="M3.5 6l4.5 4 4.5-4" fill="none" stroke="currentColor" stroke-width="1.5" />
            </svg>
          </button>

          <div v-if="menuOpen" class="top-menu-panel" role="menu">
            <button v-if="isAdmin" class="top-menu-item" type="button" @click="goTo('/admin')">
              <svg class="menu-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path
                  d="M8 1.5l5.5 2v4.4c0 3-2.3 5.7-5.5 6.6-3.2-.9-5.5-3.6-5.5-6.6V3.5L8 1.5zM8 4.2a2.3 2.3 0 100 4.6 2.3 2.3 0 000-4.6z"
                  fill="currentColor"
                />
              </svg>
              <span>后台</span>
            </button>

            <button class="top-menu-item" type="button" @click="goTo('/profile')">
              <svg class="menu-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path
                  d="M8 2.2a3 3 0 110 6 3 3 0 010-6zm0 7.2c2.8 0 5 1.6 5 3.6V14H3v-.9c0-2 2.2-3.6 5-3.6z"
                  fill="currentColor"
                />
              </svg>
              <span>档案</span>
            </button>

            <button class="top-menu-item" type="button" @click="goTo('/settings')">
              <svg class="menu-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path
                  d="M7 1h2l.4 1.7a5.9 5.9 0 011.5.9l1.6-.7 1.4 1.4-.7 1.6c.4.5.7 1 .9 1.5L15 8v2l-1.7.4a5.9 5.9 0 01-.9 1.5l.7 1.6-1.4 1.4-1.6-.7a5.9 5.9 0 01-1.5.9L9 15H7l-.4-1.7a5.9 5.9 0 01-1.5-.9l-1.6.7-1.4-1.4.7-1.6a5.9 5.9 0 01-.9-1.5L1 10V8l1.7-.4c.2-.5.5-1 .9-1.5l-.7-1.6L4.3 3l1.6.7c.5-.4 1-.7 1.5-.9L7 1zm1 4a3 3 0 100 6 3 3 0 000-6z"
                  fill="currentColor"
                />
              </svg>
              <span>设置</span>
            </button>

            <button class="top-menu-item danger" type="button" @click="handleLogout">
              <svg class="menu-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path
                  d="M2 2h6v2H4v8h4v2H2V2zm8.6 2.3L14 7.7l-3.4 3.4-1.4-1.4L10.2 8H6V6h4.2l-1-1.7 1.4-1.4z"
                  fill="currentColor"
                />
              </svg>
              <span>登出</span>
            </button>
          </div>
        </div>
      </div>
    </header>
    <div v-if="showRefreshToast" class="token-refresh-toast">{{ refreshToastText }}</div>
    <router-view />
  </div>
</template>
