<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import {
  clearAuthToken,
  fetchCurrentUserProfile,
  getAuthToken,
  saveAuthToken,
  updateCurrentUserBasic,
  updateCurrentUserPassword,
  updateCurrentUserProfile
} from "../api";

const router = useRouter();
const tokenInfo = getAuthToken();
const profile = ref(null);
const profileLoading = ref(false);
const profileError = ref("");

const tabs = [
  { key: "basic", label: "基础信息", desc: "昵称、邮箱与默认代码公开" },
  { key: "profile", label: "个人简介", desc: "编辑个人主页内容（支持 Markdown）" },
  { key: "password", label: "修改密码", desc: "更新登录密码" }
];
const activeTab = ref("basic");

const basicForm = reactive({
  nickname: "",
  email: "",
  defaultCodePublic: false
});

const profileForm = reactive({
  profile: ""
});

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});

const savingBasic = ref(false);
const savingProfile = ref(false);
const savingPassword = ref(false);

const basicFeedback = reactive({ type: "", text: "" });
const profileFeedback = reactive({ type: "", text: "" });
const passwordFeedback = reactive({ type: "", text: "" });

const username = computed(() => {
  if (typeof profile.value?.username === "string" && profile.value.username.trim()) {
    return profile.value.username.trim();
  }
  if (typeof tokenInfo?.username === "string" && tokenInfo.username.trim()) {
    return tokenInfo.username.trim();
  }
  return "-";
});
const profileLength = computed(() => String(profileForm.profile || "").length);

const activeFeedback = computed(() => {
  if (activeTab.value === "basic") {
    return basicFeedback;
  }
  if (activeTab.value === "profile") {
    return profileFeedback;
  }
  return passwordFeedback;
});

function setFeedback(section, type, text) {
  const target =
    section === "basic" ? basicFeedback : section === "profile" ? profileFeedback : passwordFeedback;
  target.type = type;
  target.text = text;
}

function syncLocalNickname(nickname) {
  const tokenPayload = getAuthToken();
  if (!tokenPayload) {
    return;
  }
  const keepLogin = tokenPayload._storageType !== "session";
  const { _storageType, ...rest } = tokenPayload;
  saveAuthToken(
    {
      ...rest,
      nickname
    },
    keepLogin
  );
}

async function loadProfile() {
  if (!tokenInfo) {
    return;
  }
  profileLoading.value = true;
  profileError.value = "";
  try {
    const data = await fetchCurrentUserProfile();
    profile.value = data;
    basicForm.nickname = typeof data?.nickname === "string" ? data.nickname : "";
    basicForm.email = typeof data?.email === "string" ? data.email : "";
    basicForm.defaultCodePublic = data?.defaultCodePublic === true;
    profileForm.profile = typeof data?.profile === "string" ? data.profile : "";
  } catch (error) {
    profileError.value = error.message || "加载用户信息失败。";
  } finally {
    profileLoading.value = false;
  }
}

async function saveBasic() {
  const nickname = basicForm.nickname.trim();
  const email = basicForm.email.trim();

  if (!nickname) {
    setFeedback("basic", "error", "昵称不能为空。");
    return;
  }
  if (nickname.length > 64) {
    setFeedback("basic", "error", "昵称长度不能超过 64 个字符。");
    return;
  }
  if (email.length > 191) {
    setFeedback("basic", "error", "邮箱长度不能超过 191 个字符。");
    return;
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    setFeedback("basic", "error", "邮箱格式不正确。");
    return;
  }

  savingBasic.value = true;
  setFeedback("basic", "", "");
  try {
    await updateCurrentUserBasic({
      nickname,
      email,
      defaultCodePublic: basicForm.defaultCodePublic
    });
    syncLocalNickname(nickname);
    await loadProfile();
    setFeedback("basic", "success", "基础信息已保存。");
  } catch (error) {
    setFeedback("basic", "error", error.message || "保存失败，请稍后重试。");
  } finally {
    savingBasic.value = false;
  }
}

async function saveProfileText() {
  const nextProfile = String(profileForm.profile ?? "");
  if (nextProfile.length > 65535) {
    setFeedback("profile", "error", "个人简介内容过长，请精简后重试。");
    return;
  }

  savingProfile.value = true;
  setFeedback("profile", "", "");
  try {
    await updateCurrentUserProfile({
      profile: nextProfile
    });
    if (profile.value) {
      profile.value = {
        ...profile.value,
        profile: nextProfile
      };
    }
    setFeedback("profile", "success", "个人简介已保存。");
  } catch (error) {
    setFeedback("profile", "error", error.message || "保存失败，请稍后重试。");
  } finally {
    savingProfile.value = false;
  }
}

async function savePassword() {
  const oldPassword = passwordForm.oldPassword;
  const newPassword = passwordForm.newPassword;
  const confirmPassword = passwordForm.confirmPassword;

  if (!oldPassword.trim()) {
    setFeedback("password", "error", "旧密码不能为空。");
    return;
  }
  if (!newPassword.trim()) {
    setFeedback("password", "error", "新密码不能为空。");
    return;
  }
  if (newPassword.length < 6 || newPassword.length > 64) {
    setFeedback("password", "error", "新密码长度需在 6-64 位之间。");
    return;
  }
  if (newPassword !== confirmPassword) {
    setFeedback("password", "error", "两次输入的新密码不一致。");
    return;
  }
  if (oldPassword === newPassword) {
    setFeedback("password", "error", "新密码不能与旧密码相同。");
    return;
  }

  savingPassword.value = true;
  setFeedback("password", "", "");
  try {
    await updateCurrentUserPassword({
      oldPassword,
      newPassword
    });
    passwordForm.oldPassword = "";
    passwordForm.newPassword = "";
    passwordForm.confirmPassword = "";
    clearAuthToken();
    router.replace("/login");
  } catch (error) {
    setFeedback("password", "error", error.message || "密码修改失败，请稍后重试。");
  } finally {
    savingPassword.value = false;
  }
}

onMounted(() => {
  loadProfile();
});
</script>

<template>
  <div class="page-bg">
    <main class="content-shell settings-shell">
      <div v-if="!tokenInfo" class="card-panel draft-card">
        <h1 class="draft-title">用户设置</h1>
        <p class="draft-subtitle">请先登录后再修改设置。</p>
        <div class="draft-empty">
          <p>当前未登录，暂无法进入设置页面。</p>
          <router-link class="top-auth-btn" to="/login">前往登录</router-link>
        </div>
      </div>

      <section v-else class="settings-layout">
        <aside class="card-panel settings-tabs">
          <h2>设置中心</h2>
          <p>@{{ username }}</p>
          <button
            v-for="item in tabs"
            :key="item.key"
            class="settings-tab-btn"
            :class="{ active: activeTab === item.key }"
            type="button"
            @click="activeTab = item.key"
          >
            <span>{{ item.label }}</span>
            <small>{{ item.desc }}</small>
          </button>
        </aside>

        <section class="card-panel settings-main">
          <div v-if="profileLoading" class="settings-loading">正在加载用户信息...</div>
          <div v-else-if="profileError" class="settings-loading error">
            <p>{{ profileError }}</p>
            <button class="refresh-btn" type="button" @click="loadProfile">重试加载</button>
          </div>

          <header class="settings-head">
            <h1>{{ tabs.find((tab) => tab.key === activeTab)?.label }}</h1>
            <p>{{ tabs.find((tab) => tab.key === activeTab)?.desc }}</p>
          </header>

          <p v-if="activeFeedback.text" class="feedback" :class="activeFeedback.type">
            {{ activeFeedback.text }}
          </p>

          <div v-if="activeTab === 'basic'" class="settings-section">
            <div class="settings-grid">
              <label>
                昵称
                <input v-model="basicForm.nickname" class="settings-input" maxlength="64" />
              </label>
              <label>
                邮箱
                <input
                  v-model="basicForm.email"
                  class="settings-input"
                  maxlength="191"
                  placeholder="留空表示解绑邮箱"
                />
              </label>
            </div>

            <label class="settings-check">
              <input v-model="basicForm.defaultCodePublic" type="checkbox" />
              <span>默认代码公开</span>
            </label>

            <div class="settings-actions">
              <button
                class="submit-btn"
                type="button"
                :disabled="savingBasic || profileLoading"
                @click="saveBasic"
              >
                {{ savingBasic ? "保存中..." : "保存基础信息" }}
              </button>
            </div>
          </div>

          <div v-else-if="activeTab === 'profile'" class="settings-section">
            <label class="settings-block">
              个人简介
              <textarea
                v-model="profileForm.profile"
                class="settings-textarea settings-textarea-lg"
                maxlength="65535"
                placeholder="介绍一下你自己..."
              />
            </label>
            <small class="settings-inline-tip">当前长度：{{ profileLength }} / 65535</small>

            <div class="settings-actions">
              <button
                class="submit-btn"
                type="button"
                :disabled="savingProfile || profileLoading"
                @click="saveProfileText"
              >
                {{ savingProfile ? "保存中..." : "保存个人简介" }}
              </button>
            </div>
          </div>

          <div v-else class="settings-section">
            <div class="settings-grid">
              <label>
                旧密码
                <input
                  v-model="passwordForm.oldPassword"
                  class="settings-input"
                  type="password"
                  autocomplete="current-password"
                />
              </label>
              <label>
                新密码
                <input
                  v-model="passwordForm.newPassword"
                  class="settings-input"
                  type="password"
                  autocomplete="new-password"
                />
              </label>
              <label>
                确认新密码
                <input
                  v-model="passwordForm.confirmPassword"
                  class="settings-input"
                  type="password"
                  autocomplete="new-password"
                />
              </label>
            </div>
            <small class="settings-inline-tip">新密码长度需在 6-64 位之间。</small>

            <div class="settings-actions">
              <button
                class="submit-btn"
                type="button"
                :disabled="savingPassword || profileLoading"
                @click="savePassword"
              >
                {{ savingPassword ? "提交中..." : "修改密码" }}
              </button>
            </div>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>
