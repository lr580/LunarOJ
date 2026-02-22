<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  AUTH_REFRESH_NOTICE_CONFIG,
  AUTH_TOKEN_CHANGED_EVENT,
  fetchCurrentUserProfile,
  getAuthToken,
  getAuthTokenExpireAt,
  getRefreshTokenExpireAt
} from "../api";
import { renderMarkdown } from "../utils/markdown";

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

const tokenInfo = getAuthToken();
const debugTokenInfo = ref(getAuthToken());
const profile = ref(null);
const profileLoading = ref(false);
const profileError = ref("");
const jwtPayload = parseJwtPayload(tokenInfo?.accessToken);

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const dt = new Date(value);
  if (Number.isNaN(dt.getTime())) {
    return String(value);
  }
  return dt.toLocaleString("zh-CN", { hour12: false });
}

async function loadProfile() {
  if (!tokenInfo) {
    return;
  }
  profileLoading.value = true;
  profileError.value = "";
  try {
    profile.value = await fetchCurrentUserProfile();
  } catch (error) {
    profileError.value = error.message || "加载用户信息失败。";
  } finally {
    profileLoading.value = false;
  }
}

onMounted(() => {
  loadProfile();
  if (typeof window !== "undefined") {
    window.addEventListener(AUTH_TOKEN_CHANGED_EVENT, syncDebugTokenInfo);
  }
});

onBeforeUnmount(() => {
  if (typeof window !== "undefined") {
    window.removeEventListener(AUTH_TOKEN_CHANGED_EVENT, syncDebugTokenInfo);
  }
});

function syncDebugTokenInfo() {
  debugTokenInfo.value = getAuthToken();
}

const displayName = computed(() => {
  if (!tokenInfo) {
    return "";
  }
  if (typeof profile.value?.nickname === "string" && profile.value.nickname.trim()) {
    return profile.value.nickname.trim();
  }
  if (typeof tokenInfo.nickname === "string" && tokenInfo.nickname.trim()) {
    return tokenInfo.nickname.trim();
  }
  if (typeof profile.value?.username === "string" && profile.value.username.trim()) {
    return profile.value.username.trim();
  }
  if (typeof tokenInfo.username === "string" && tokenInfo.username.trim()) {
    return tokenInfo.username.trim();
  }
  if (typeof jwtPayload?.username === "string" && jwtPayload.username.trim()) {
    return jwtPayload.username.trim();
  }
  return "未命名用户";
});

const username = computed(() => {
  if (typeof profile.value?.username === "string" && profile.value.username.trim()) {
    return profile.value.username.trim();
  }
  if (typeof tokenInfo?.username === "string" && tokenInfo.username.trim()) {
    return tokenInfo.username.trim();
  }
  if (typeof jwtPayload?.username === "string" && jwtPayload.username.trim()) {
    return jwtPayload.username.trim();
  }
  return "-";
});

watch(
  username,
  (value) => {
    const safeUsername = String(value || "").trim();
    if (!safeUsername || safeUsername === "-") {
      document.title = "个人主页 - LunarOJ";
      return;
    }
    document.title = `${safeUsername} - LunarOJ`;
  },
  { immediate: true }
);

const userGroup = computed(() => {
  if (
    typeof profile.value?.permissionGroupName === "string" &&
    profile.value.permissionGroupName.trim()
  ) {
    return profile.value.permissionGroupName.trim();
  }
  if (typeof profile.value?.permissionGroupId === "number") {
    return `权限组 #${profile.value.permissionGroupId}`;
  }
  const groupName = jwtPayload?.permissionGroupName || jwtPayload?.permissionGroup || jwtPayload?.role;
  if (typeof groupName === "string" && groupName.trim()) {
    return groupName.trim();
  }
  return "普通用户";
});

const userEmail = computed(() => {
  if (typeof profile.value?.email === "string" && profile.value.email.trim()) {
    return profile.value.email.trim();
  }
  return "未绑定";
});

const userProfileText = computed(() => {
  if (typeof profile.value?.profile === "string" && profile.value.profile.trim()) {
    return profile.value.profile.trim();
  }
  return "暂无个人简介。";
});
const userProfileHtml = computed(() => renderMarkdown(userProfileText.value));

const createdAtText = computed(() => formatDateTime(profile.value?.createdAt));
const lastLoginText = computed(() => formatDateTime(profile.value?.lastLoginAt));
const isDebugMode = computed(() => AUTH_REFRESH_NOTICE_CONFIG.enabled === true);
const accessTokenExpireAtText = computed(() => {
  const expireAt = getAuthTokenExpireAt(debugTokenInfo.value);
  return formatDateTime(expireAt || null);
});
const refreshTokenExpireAtText = computed(() => {
  const expireAt = getRefreshTokenExpireAt(debugTokenInfo.value);
  return formatDateTime(expireAt || null);
});
</script>

<template>
  <div class="page-bg">
    <main class="content-shell profile-shell">
      <div v-if="!tokenInfo" class="card-panel draft-card">
        <h1 class="draft-title">用户档案</h1>
        <p class="draft-subtitle">当前未登录，暂无法展示档案内容。</p>
        <div class="draft-empty">
          <p>请先登录后查看个人档案。</p>
          <router-link class="top-auth-btn" to="/login">前往登录</router-link>
        </div>
      </div>

      <template v-else>
        <div v-if="profileLoading" class="card-panel draft-card">
          <h1 class="draft-title">用户档案</h1>
          <p class="draft-subtitle">正在加载用户信息...</p>
        </div>

        <div v-else-if="profileError" class="card-panel draft-card">
          <h1 class="draft-title">用户档案</h1>
          <p class="draft-subtitle">用户信息加载失败</p>
          <p class="feedback error">{{ profileError }}</p>
          <div class="draft-empty">
            <button class="refresh-btn" type="button" @click="loadProfile">重试加载</button>
          </div>
        </div>

        <template v-else>
        <section class="profile-grid">
          <article class="card-panel profile-card">
            <div class="profile-card-head">
              <h2>账号信息</h2>
              <router-link class="profile-icon-btn" to="/settings" title="编辑资料" aria-label="编辑资料">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm16.71-10.04a1 1 0 0 0 0-1.41l-1.5-1.5a1 1 0 0 0-1.41 0l-1.13 1.13 3.75 3.75 1.29-1.21z"
                    fill="currentColor"
                  />
                </svg>
              </router-link>
            </div>
            <dl class="profile-info-list">
              <div>
                <dt>用户名</dt>
                <dd>
                  <router-link class="page-link" :to="`/users/${encodeURIComponent(username)}`">
                    {{ username }}
                  </router-link>
                </dd>
              </div>
              <div>
                <dt>昵称</dt>
                <dd>{{ displayName }}</dd>
              </div>
              <div>
                <dt>权限组</dt>
                <dd>{{ userGroup }}</dd>
              </div>
              <div>
                <dt>邮箱</dt>
                <dd>
                  {{ userEmail }}
                  <small v-if="profile?.emailVerified === true" class="profile-inline-ok">（已验证）</small>
                </dd>
              </div>
              <div>
                <dt>账号创建时间</dt>
                <dd>{{ createdAtText }}</dd>
              </div>
              <div>
                <dt>最近登录时间</dt>
                <dd>{{ lastLoginText }}</dd>
              </div>
              <div>
                <dt>默认代码公开</dt>
                <dd>{{ profile?.defaultCodePublic ? "是" : "否" }}</dd>
              </div>
            </dl>
            <dl v-if="isDebugMode" class="profile-info-list profile-debug-box">
              <div>
                <dt>Access Token 到期时间</dt>
                <dd>{{ accessTokenExpireAtText }}</dd>
              </div>
              <div>
                <dt>Refresh Token 到期时间</dt>
                <dd>{{ refreshTokenExpireAtText }}</dd>
              </div>
            </dl>
          </article>

          <article class="card-panel profile-card profile-bio-card">
            <h2>个人简介</h2>
            <div class="profile-bio-box">
              <div class="md-content" v-html="userProfileHtml"></div>
            </div>
          </article>
        </section>
        </template>
      </template>
    </main>
  </div>
</template>
