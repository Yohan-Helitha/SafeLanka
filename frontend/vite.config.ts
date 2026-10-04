import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import type { ProxyOptions } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // Where the Spring Boot API runs. Set VITE_PROXY_TARGET in .env.local if your port differs.
  const api = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  // /api is proxied, so the browser talks only to the dev server: the httpOnly refresh cookie stays
  // same-origin (also from a phone on the LAN) and no CORS setup is needed in development.
  const proxy: Record<string, ProxyOptions> = {
    '/api': {
      target: api,
      changeOrigin: true,
      configure: (p) => {
        // The page origin is not the API origin; dropping it keeps the backend from treating this as cross-origin.
        p.on('proxyReq', (req) => req.removeHeader('origin'))
      },
    },
  }

  return {
    plugins: [react()],
    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    // Listen on the LAN so `npm run dev` prints a Network URL to open on a phone.
    server: { host: true, proxy },
    preview: { host: true, proxy },
  }
})
