import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "katex/dist/katex.min.css";
import "highlight.js/styles/github.css";
import "./style.css";

createApp(App).use(router).mount("#app");
