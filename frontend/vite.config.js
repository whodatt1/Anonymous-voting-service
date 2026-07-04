import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/votes': {
        target: 'http://localhost:8080',
        bypass: (req) => {
          if (req.headers.accept?.includes('text/html')) {
            return req.url
          }
        }
      }
    }
  },
})
