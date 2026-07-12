import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy hace que en dev el front y la API sean same-origin, para que la
// cookie de sesión del flujo CSRF se preserve. En producción no hace falta:
// Spring Boot sirve el build de React y la API bajo el mismo origen.
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
