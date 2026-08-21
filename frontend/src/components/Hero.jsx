import { useState } from 'react'
import { useLanguage } from '../i18n/LanguageContext.jsx'
import RichText from './RichText.jsx'

export default function Hero() {
  const [imgError, setImgError] = useState(false)
  const { t } = useLanguage()

  return (
    <section id="hero">
      <div className="hero-grid"></div>
      <div className="hero-glow"></div>

      {/* Texto izquierda */}
      <div className="hero-left">
        <div className="badge">{t.hero.badge}</div>
        <h1>
          {t.hero.greeting}<br />
          <span className="hl">Adrián Garcés</span><br />
          <span className="out">{t.hero.role}</span>
        </h1>
        <p className="hero-desc">
          <RichText>{t.hero.desc}</RichText>
        </p>
        <div className="ctas">
          <a href="#projects" className="btn-p">{t.hero.ctaProjects}</a>
          <a href="#contact" className="btn-s">{t.hero.ctaContact}</a>
        </div>
      </div>

      {/* Foto derecha */}
      <div className="hero-photo-wrap">
        <div style={{ position: 'relative', display: 'inline-block', width: '100%' }}>
          <div className="photo-corner tl"></div>
          <div className="photo-corner br"></div>
          <div className="photo-frame">

            {!imgError && (
              <picture>
                <source srcSet="/img/Avatar.webp" type="image/webp" />
                <img
                  src="/img/Avatar.jpg"
                  alt="Adrián Garcés"
                  id="profile-img"
                  width="800"
                  height="800"
                  fetchPriority="high"
                  onError={() => setImgError(true)}
                />
              </picture>
            )}

            {imgError && (
              <div className="photo-placeholder" id="placeholder" style={{ display: 'flex' }}>
                <svg viewBox="0 0 64 64" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="32" cy="22" r="12" />
                  <path d="M8 56c0-13.255 10.745-24 24-24s24 10.745 24 24" />
                </svg>
                <span>{t.hero.noPhoto}</span>
              </div>
            )}

          </div>
        </div>
        {/* Chips bajo la foto */}
        <div className="photo-chips">
          {t.hero.chips.map((chip, i) => (
            <span key={chip} className={`chip${i === 0 ? ' active' : ''}`}>{chip}</span>
          ))}
        </div>
      </div>
    </section>
  )
}
