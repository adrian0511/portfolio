import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const SITE_URL = 'https://adrian0511.dev'

// Los buscadores leen la frescura de la página en dos sitios: dateModified (JSON-LD)
// y <lastmod> (sitemap). Se calcula una sola vez por build para que ambos coincidan;
// si no, un crawler ve fechas distintas para el mismo documento y descarta la señal.
const BUILD_TIME = new Date().toISOString()

function seo() {
  return {
    name: 'portfolio-seo',
    transformIndexHtml(html) {
      return html.replaceAll('%BUILD_TIME%', BUILD_TIME)
    },
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: 'sitemap.xml',
        source: `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>${SITE_URL}/</loc>
    <lastmod>${BUILD_TIME}</lastmod>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
</urlset>
`,
      })
    },
  }
}

// El proxy hace que en dev el front y la API sean same-origin, para que la
// cookie de sesión del flujo CSRF se preserve. En producción no hace falta:
// Spring Boot sirve el build de React y la API bajo el mismo origen.
export default defineConfig({
  plugins: [react(), seo()],
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
