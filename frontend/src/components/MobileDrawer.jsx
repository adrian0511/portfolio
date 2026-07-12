import { useLanguage } from '../i18n/LanguageContext.jsx'

export default function MobileDrawer({ open, onClose }) {
  const { t } = useLanguage()

  return (
    <div className={`drawer${open ? ' open' : ''}`} id="drawer">
      <a href="#about" onClick={onClose}>{t.nav.about}</a>
      <a href="#projects" onClick={onClose}>{t.nav.projects}</a>
      <a href="#contact" onClick={onClose}>{t.nav.contact}</a>
      <div className="drawer-sub">
        <a href="https://github.com/adrian0511" target="_blank" rel="noreferrer">GitHub</a>
        <a href="https://linkedin.com/in/adrdev" target="_blank" rel="noreferrer">LinkedIn</a>
      </div>
    </div>
  )
}
