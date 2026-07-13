import { useLanguage } from '../i18n/LanguageContext.jsx'

export default function Footer() {
  const { t } = useLanguage()
  const year = new Date().getFullYear()

  return (
    <footer>
      <span>© <span id="year">{year}</span> Adrián Arsenio Garcés Jiménez — {t.footer.tagline}</span>
      <span>React · Vite · Spring Boot</span>
    </footer>
  )
}
