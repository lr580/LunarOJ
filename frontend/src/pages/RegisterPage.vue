<script setup>
import { onMounted, onUnmounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchCaptcha, fetchRegisterEnabled, registerUser } from "../api";
import logoUrl from "../assets/LunarOJ-icon.svg";

const CAPTCHA_EXPIRE_MS = 5 * 60 * 1000;
const router = useRouter();

const registerEnabled = ref(true);
const feedback = reactive({
  type: "",
  text: ""
});
const nowTs = ref(Date.now());
let captchaTimerId = null;

const loading = reactive({
  register: false,
  captchaRegister: false
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  nickname: "",
  email: "",
  captchaCode: ""
});
const registerNicknameManual = ref(false);

const captcha = reactive({
  captchaId: "",
  imageBase64: "",
  expiresAt: 0
});

function setFeedback(type, text) {
  feedback.type = type;
  feedback.text = text;
}

function handleRegisterUsernameInput() {
  const username = registerForm.username.trim();
  if (!registerNicknameManual.value || !registerForm.nickname.trim()) {
    registerForm.nickname = username;
  }
}

function handleRegisterNicknameInput() {
  const username = registerForm.username.trim();
  const nickname = registerForm.nickname.trim();
  registerNicknameManual.value = nickname !== "" && nickname !== username;
}

function handleRegisterNicknameClick() {
  const username = registerForm.username.trim();
  const nickname = registerForm.nickname.trim();
  if (username && nickname === username) {
    registerForm.nickname = "";
    registerNicknameManual.value = false;
  }
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
  loading.captchaRegister = true;
  try {
    const data = await fetchCaptcha();
    captcha.captchaId = data.captchaId;
    captcha.imageBase64 = normalizeCaptchaImageSrc(data.imageBase64);
    captcha.expiresAt = Date.now() + CAPTCHA_EXPIRE_MS;
    registerForm.captchaCode = "";
  } catch (error) {
    setFeedback("error", error.message || "获取验证码失败，请重试。");
  } finally {
    loading.captchaRegister = false;
  }
}

function handleCaptchaImageError() {
  captcha.imageBase64 = "";
  setFeedback("error", "验证码图片加载失败，请点击刷新验证码。");
}

function validateRegisterForm() {
  const usernamePattern = /^[A-Za-z0-9._-]{3,64}$/;
  if (!usernamePattern.test(registerForm.username)) {
    return "用户名需为 3-64 位，仅支持字母、数字和 -_.";
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 64) {
    return "密码长度需在 6-64 位之间。";
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    return "两次输入的密码不一致。";
  }
  if (registerForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    return "邮箱格式不正确。";
  }
  if (!registerForm.captchaCode.trim()) {
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

async function handleRegister() {
  if (!registerEnabled.value) {
    setFeedback("error", "当前暂未开放注册。");
    return;
  }

  const validationError = validateRegisterForm();
  if (validationError) {
    setFeedback("error", validationError);
    return;
  }

  loading.register = true;
  setFeedback("", "");

  try {
    const username = registerForm.username.trim();
    await registerUser({
      username,
      password: registerForm.password,
      nickname: registerForm.nickname.trim() || null,
      email: registerForm.email.trim() || null,
      captchaId: captcha.captchaId,
      captchaCode: registerForm.captchaCode.trim()
    });
    await router.push({
      path: "/login",
      query: {
        fromRegister: "1",
        username
      }
    });
    return;
  } catch (error) {
    setFeedback("error", error.message || "注册失败，请重试。");
  } finally {
    loading.register = false;
    await loadCaptcha();
  }
}

onMounted(async () => {
  captchaTimerId = setInterval(() => {
    nowTs.value = Date.now();
  }, 1000);
  await loadRegisterEnabled();
  if (registerEnabled.value) {
    await loadCaptcha();
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
          <button type="button" @click="goLogin">登录</button>
          <button v-if="registerEnabled" class="active" type="button" @click="goRegister">注册</button>
        </div>

        <p v-if="feedback.text" class="feedback" :class="feedback.type">
          {{ feedback.text }}
        </p>

        <div v-if="!registerEnabled" class="register-closed">
          <p>当前暂未开放注册。</p>
          <router-link class="submit-btn home-btn" to="/login">返回登录</router-link>
        </div>

        <form v-else class="form" @submit.prevent="handleRegister">
          <label>
            用户名
            <input
              v-model="registerForm.username"
              autocomplete="username"
              maxlength="64"
              @input="handleRegisterUsernameInput"
            />
          </label>

          <label>
            昵称（可选）
            <input
              v-model="registerForm.nickname"
              autocomplete="nickname"
              maxlength="64"
              @click="handleRegisterNicknameClick"
              @input="handleRegisterNicknameInput"
            />
          </label>

          <label>
            邮箱（可选）
            <input v-model="registerForm.email" type="email" autocomplete="email" maxlength="191" />
          </label>

          <label>
            密码
            <input
              v-model="registerForm.password"
              type="password"
              autocomplete="new-password"
              maxlength="64"
            />
          </label>

          <label>
            确认密码
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              autocomplete="new-password"
              maxlength="64"
            />
          </label>

          <label>
            验证码
            <input
              v-model="registerForm.captchaCode"
              autocomplete="off"
              maxlength="8"
              placeholder="请输入 4 位验证码"
            />
          </label>

          <div class="captcha-block">
            <img
              v-if="captcha.imageBase64"
              :src="captcha.imageBase64"
              alt="注册验证码"
              class="captcha-image"
              @error="handleCaptchaImageError"
            />
            <div v-else class="captcha-image captcha-placeholder">验证码加载失败</div>
            <button
              class="refresh-btn"
              type="button"
              :disabled="loading.captchaRegister"
              @click="loadCaptcha"
            >
              {{ loading.captchaRegister ? "刷新中..." : "刷新验证码" }}
            </button>
          </div>

          <button
            v-if="isCaptchaExpired()"
            class="captcha-expired-tip"
            type="button"
            @click="loadCaptcha"
          >
            验证码已过期，点击刷新验证码
          </button>

          <button class="submit-btn" type="submit" :disabled="loading.register">
            {{ loading.register ? "提交中..." : "注册" }}
          </button>
        </form>
      </section>
    </main>
  </div>
</template>
