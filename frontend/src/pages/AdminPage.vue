<script setup>
import { computed } from "vue";
import { getAuthToken } from "../api";

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
const jwtPayload = parseJwtPayload(tokenInfo?.accessToken) || {};

const username = computed(() => {
  const fromTokenInfo = typeof tokenInfo?.username === "string" ? tokenInfo.username.trim() : "";
  if (fromTokenInfo) {
    return fromTokenInfo;
  }
  const fromJwt = typeof jwtPayload.username === "string" ? jwtPayload.username.trim() : "";
  return fromJwt || "";
});

const isAdmin = computed(() => {
  const candidates = [];
  if (Array.isArray(jwtPayload.roles)) {
    candidates.push(...jwtPayload.roles);
  }
  if (Array.isArray(jwtPayload.authorities)) {
    candidates.push(...jwtPayload.authorities);
  }
  if (typeof jwtPayload.role === "string") {
    candidates.push(jwtPayload.role);
  }
  if (typeof jwtPayload.permissionGroup === "string") {
    candidates.push(jwtPayload.permissionGroup);
  }
  if (typeof jwtPayload.permissionGroupName === "string") {
    candidates.push(jwtPayload.permissionGroupName);
  }
  const normalized = candidates.map((item) => String(item || "").toUpperCase());
  return normalized.some((item) => item.includes("ADMIN") || item.includes("ROOT"));
});
</script>

<template>
  <div class="page-bg">
    <main class="content-shell">
      <section class="card-panel draft-card">
        <h1 class="draft-title">后台管理</h1>
        <p class="draft-subtitle">草稿页面，后续将接入管理接口与权限校验。</p>

        <div v-if="!tokenInfo" class="draft-empty">
          <p>未登录，无法访问后台页面。</p>
          <router-link class="top-auth-btn" to="/login">前往登录</router-link>
        </div>

        <div v-else-if="!isAdmin" class="draft-empty">
          <p>当前账号不是管理员，暂无权限访问此页面。</p>
        </div>

        <div v-else class="draft-grid">
          <article class="draft-grid-item">
            <h2>用户管理</h2>
            <p>草稿：封禁用户、角色调整、账号搜索。</p>
          </article>
          <article class="draft-grid-item">
            <h2>题目管理</h2>
            <p>草稿：新增题目、编辑题目、批量导入。</p>
          </article>
          <article class="draft-grid-item">
            <h2>评测管理</h2>
            <p>草稿：评测队列、重判、节点状态。</p>
          </article>
          <article class="draft-grid-item">
            <h2>系统配置</h2>
            <p>草稿：注册开关、公告、站点参数。</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>
