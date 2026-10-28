import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev the browser talks to Vite, which proxies /gateway/* to the API gateway.
// Same-origin requests => no CORS configuration needed. nginx does the same in Docker.
const gateway = process.env.GATEWAY_URL || "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/gateway": {
        target: gateway,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/gateway/, ""),
      },
    },
  },
});
