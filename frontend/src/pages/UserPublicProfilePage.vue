<script setup>
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { fetchUserPublicProfile } from "../api";
import { renderMarkdown } from "../utils/markdown";

const route = useRoute();
const profile = ref(null);
const loading = ref(false);
const errorText = ref("");

function pickFirstQueryValue(value) {
  return Array.isArray(value) ? value[0] : value;
}

function normalizeView(value) {
  const text = String(value || "").trim().toLowerCase();
  if (text === "basic" || text === "info") {
    return "basic";
  }
  if (text === "profile" || text === "bio") {
    return "profile";
  }
  return "all";
}

function resolveViewFromRoute() {
  const queryView = pickFirstQueryValue(route.query.view ?? route.query.tab);
  if (queryView != null && String(queryView).trim()) {
    return normalizeView(queryView);
  }
  const hashView = String(route.hash || "").replace(/^#/, "");
  if (hashView) {
    return normalizeView(hashView);
  }
  return "all";
}

function setPublicTitle(name) {
  const titleName = String(name || "").trim();
  document.title = `${titleName || "用户主页"} - LunarOJ`;
}

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

async function loadProfile(username) {
  const target = String(username || "").trim();
  if (!target) {
    profile.value = null;
    errorText.value = "用户名不能为空。";
    return;
  }

  loading.value = true;
  errorText.value = "";
  try {
    profile.value = await fetchUserPublicProfile(target);
    setPublicTitle(profile.value.username || target);
  } catch (error) {
    profile.value = null;
    errorText.value = error.message || "加载用户主页失败。";
    document.title = "用户主页 - LunarOJ";
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.username,
  (value) => {
    const target = Array.isArray(value) ? value[0] : value;
    loadProfile(target);
  },
  { immediate: true }
);

const activeView = computed(() => resolveViewFromRoute());
const singleView = computed(() => activeView.value !== "all");
const createdAtText = computed(() => formatDateTime(profile.value?.createdAt));
const permissionGroupName = computed(() => {
  const text = String(profile.value?.permissionGroupName || "").trim();
  return text || "普通用户";
});
const publicProfileText = computed(() => {
  if (typeof profile.value?.profile === "string" && profile.value.profile.trim()) {
    return profile.value.profile.trim();
  }
  return "该用户暂未填写个人简介。";
});
const publicProfileHtml = computed(() => renderMarkdown(publicProfileText.value));

watch(activeView, () => {
  if (profile.value?.username) {
    setPublicTitle(profile.value.username);
  }
});
</script>

<template>
  <div class="page-bg">
    <main class="content-shell profile-shell">
      <section v-if="loading" class="card-panel draft-card">
        <p class="draft-subtitle">正在加载用户主页...</p>
      </section>

      <section v-else-if="errorText" class="card-panel draft-card">
        <h2 class="draft-title">未找到用户主页</h2>
        <p class="feedback error">{{ errorText }}</p>
      </section>

      <section
        v-else-if="profile"
        class="profile-grid public-profile-grid"
        :class="{ single: singleView }"
      >
        <article v-if="activeView === 'basic' || activeView === 'all'" class="card-panel profile-card">
          <h2>账号信息</h2>
          <dl class="profile-info-list">
            <div>
              <dt>用户名</dt>
              <dd>{{ profile.username }}</dd>
            </div>
            <div>
              <dt>昵称</dt>
              <dd>{{ profile.nickname || profile.username }}</dd>
            </div>
            <div>
              <dt>权限组</dt>
              <dd>{{ permissionGroupName }}</dd>
            </div>
            <div>
              <dt>加入时间</dt>
              <dd>{{ createdAtText }}</dd>
            </div>
          </dl>
        </article>

        <article
          v-if="activeView === 'profile' || activeView === 'all'"
          class="card-panel profile-card profile-bio-card"
        >
          <h2>个人简介</h2>
          <div class="profile-bio-box">
            <div class="md-content" v-html="publicProfileHtml"></div>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>
