import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Local dev proxy to the Java admin server.
// Adjust target port if your `easysearch-admin` runs elsewhere.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

