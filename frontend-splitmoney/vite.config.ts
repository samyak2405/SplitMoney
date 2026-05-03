import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  envPrefix: ['VITE_', 'GOOGLE_OAUTH_'],
  server: {
    port: 5173,
    strictPort: false, // increment to 5174 if 5173 is taken rather than failing
    proxy: {
      '/api/splitwise': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/v1/expenses': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/docs': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/chat': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/ai': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      '/v1/payments': {
        target: 'http://localhost:8086',
        changeOrigin: true,
      },
      '/ws-notifications': {
        target: 'http://localhost:8083',
        ws: true,
        changeOrigin: true,
      },
      '/ws-chat': {
        target: 'http://localhost:8085',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
