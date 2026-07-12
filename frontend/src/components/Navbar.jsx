import { useEffect, useRef } from 'react'
import { useLanguage } from '../i18n/LanguageContext.jsx'
import LanguageToggle from './LanguageToggle.jsx'

// Nav fijo que se vuelve "solid" tras 40px de scroll.
// El toggle de idioma va a la derecha (visible también en mobile, junto a la hamburguesa).
export default function Navbar({ drawerOpen, onToggleDrawer }) {
  const navRef = useRef(null)
  const { t } = useLanguage()

  useEffect(() => {
    const nav = navRef.current
    const onScroll = () => {
      nav.classList.toggle('solid', window.scrollY > 40)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll() // estado inicial
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <nav id="nav" ref={navRef}>
      <div className="logo">dev<em>_portfolio</em></div>

      <div className="nav-right">
        <ul className="nav-links">
          <li><a href="#about">{t.nav.about}</a></li>
          <li><a href="#projects">{t.nav.projects}</a></li>
          <li><a href="#contact">{t.nav.contact}</a></li>
        </ul>

        <LanguageToggle />

        <button
          className={`hbg${drawerOpen ? ' x' : ''}`}
          id="hbg"
          aria-label={t.nav.menu}
          aria-expanded={drawerOpen}
          onClick={onToggleDrawer}
        >
          <span></span><span></span><span></span>
        </button>
      </div>
    </nav>
  )
}
