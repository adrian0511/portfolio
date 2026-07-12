import { useLanguage } from '../i18n/LanguageContext.jsx'

// Footer con año dinámico.
export default function Footer() {
  const { t } = useLanguage()
  const year = new Date().getFullYear()

  return (
    <footer>
      <span>© <span id="year">{year}</span> Adrián Garcés — {t.footer.tagline}</span>
      <span>React · Vite · Spring Boot</span>
    </footer>
  )
}
