import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules")) {
            if (
              id.includes("katex") ||
              id.includes("markdown-it") ||
              id.includes("markdown-it-texmath") ||
              id.includes("highlight.js") ||
              id.includes("dompurify")
            ) {
              return "md-vendor";
            }
          }
          return undefined;
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true
      }
    }
  }
});
