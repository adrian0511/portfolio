import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Config de desarrollo:
// - El proxy /api -> http://localhost:8080 permite consumir el backend Spring Boot
//   preservando la cookie de sesion (necesaria para el flujo CSRF).
//   Se usara al cablear la data (paso 5-6). Por ahora es inofensivo.
// - build.outDir apunta al static/ de Spring Boot para el build integrado (paso 7-8).
//   Comentado hasta que validemos; por defecto Vite compila a frontend/dist.
export default defineConfig({
  plugins: [react()],
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
