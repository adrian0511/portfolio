import { useLanguage } from '../i18n/LanguageContext.jsx'

// Banderas en SVG inline (no emoji: los emoji de bandera no se renderizan
// como banderas en Windows, se verían como las letras "ES"/"GB").

function FlagES() {
  return (
    <svg viewBox="0 0 60 40" className="flag" aria-hidden="true" focusable="false">
      <rect width="60" height="40" fill="#c60b1e" />
      <rect y="10" width="60" height="20" fill="#ffc400" />
    </svg>
  )
}

function FlagGB() {
  return (
    <svg viewBox="0 0 60 30" className="flag" aria-hidden="true" focusable="false">
      <clipPath id="gb-clip">
        <path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z" />
      </clipPath>
      <rect width="60" height="30" fill="#00247d" />
      <path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6" />
      <path d="M0,0 L60,30 M60,0 L0,30" clipPath="url(#gb-clip)" stroke="#cf142b" strokeWidth="4" />
      <path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10" />
      <path d="M30,0 v30 M0,15 h60" stroke="#cf142b" strokeWidth="6" />
    </svg>
  )
}

export default function LanguageToggle() {
  const { lang, setLang } = useLanguage()

  return (
    <div className="lang-toggle" role="group" aria-label="Idioma / Language">
      <button
        type="button"
        className={lang === 'es' ? 'active' : ''}
        onClick={() => setLang('es')}
        aria-pressed={lang === 'es'}
        title="Español"
      >
        <FlagES />
        <span>ES</span>
      </button>

      <button
        type="button"
        className={lang === 'en' ? 'active' : ''}
        onClick={() => setLang('en')}
        aria-pressed={lang === 'en'}
        title="English"
      >
        <FlagGB />
        <span>EN</span>
      </button>
    </div>
  )
}
