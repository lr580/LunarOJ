import { createRouter, createWebHistory } from "vue-router";
import HomePage from "./pages/HomePage.vue";
import LoginPage from "./pages/LoginPage.vue";
import RegisterPage from "./pages/RegisterPage.vue";
import ProfilePage from "./pages/ProfilePage.vue";
import SettingsPage from "./pages/SettingsPage.vue";
import AdminPage from "./pages/AdminPage.vue";
import UserPublicProfilePage from "./pages/UserPublicProfilePage.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: HomePage,
      meta: { title: "首页 - LunarOJ" }
    },
    {
      path: "/login",
      component: LoginPage,
      meta: { title: "登录 - LunarOJ" }
    },
    {
      path: "/register",
      component: RegisterPage,
      meta: { title: "注册 - LunarOJ" }
    },
    {
      path: "/profile",
      component: ProfilePage,
      meta: { title: "个人主页 - LunarOJ", dynamicTitle: true }
    },
    {
      path: "/settings",
      component: SettingsPage,
      meta: { title: "设置 - LunarOJ" }
    },
    {
      path: "/admin",
      component: AdminPage,
      meta: { title: "后台 - LunarOJ" }
    },
    {
      path: "/users/:username",
      component: UserPublicProfilePage,
      meta: { title: "用户主页 - LunarOJ", dynamicTitle: true }
    },
    {
      path: "/:pathMatch(.*)*",
      redirect: "/"
    }
  ]
});

router.afterEach((to) => {
  if (to.meta?.dynamicTitle) {
    return;
  }
  document.title = to.meta?.title || "LunarOJ";
});

export default router;
