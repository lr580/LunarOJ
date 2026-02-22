# 前端项目（LunarOJ）

## 启动方式

```bash
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`

## 后端接口代理

开发环境下，Vite 会将 `/api/*` 代理到 `http://127.0.0.1:8080`。

如果后端地址或端口有变化，请修改 `vite.config.js` 中的 `server.proxy` 配置。

## 当前页面入口

- `/`：首页（临时草稿页）
- `/login`：登录页
- `/register`：注册页
- `/profile`：当前用户主页（需登录）
- `/settings`：用户设置（需登录）
- `/users/:username`：公开用户主页
- `/admin`：后台草稿页
