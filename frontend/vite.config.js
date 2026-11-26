import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Same origin as the API, so the browser sends the refresh cookie
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
