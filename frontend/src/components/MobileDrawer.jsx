import { FiMessageSquare } from 'react-icons/fi'
import { useLanguage } from '../i18n/LanguageContext.jsx'

export default function MobileDrawer({ open, onClose, onOpenChat }) {
  const { t } = useLanguage()

  // inert y no solo opacity/pointer-events: por debajo del breakpoint el drawer
  // es display:flex tambien cerrado, y ni opacity:0 ni pointer-events:none sacan
  // nada del orden de tabulacion. Sin esto, al tabular desde la hamburguesa el
  // foco se iba a seis elementos invisibles fuera de pantalla.
  return (
    <div
      className={`drawer${open ? ' open' : ''}`}
      id="drawer"
      inert={open ? undefined : ''}
      aria-hidden={open ? undefined : 'true'}
    >
      <a href="#about" onClick={onClose}>{t.nav.about}</a>
      <a href="#projects" onClick={onClose}>{t.nav.projects}</a>
      <a href="#contact" onClick={onClose}>{t.nav.contact}</a>
      <button type="button" className="drawer-chat" onClick={onOpenChat}>
        <FiMessageSquare aria-hidden="true" />
        {t.nav.chat}
      </button>
      <div className="drawer-sub">
        <a href="https://github.com/adrian0511" target="_blank" rel="noreferrer">GitHub</a>
        <a href="https://linkedin.com/in/adrdev" target="_blank" rel="noreferrer">LinkedIn</a>
      </div>
    </div>
  )
}
