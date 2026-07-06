import { useState } from 'react'

// Hero. Si la foto falla al cargar, se muestra el placeholder
// (equivalente al onerror del <img> original).
export default function Hero() {
  const [imgError, setImgError] = useState(false)

  return (
    <section id="hero">
      <div className="hero-grid"></div>
      <div className="hero-glow"></div>

      {/* Texto izquierda */}
      <div className="hero-left">
        <div className="badge">Disponible para trabajar</div>
        <h1>
          Hola, soy<br />
          <span className="hl">Adrián Garcés</span><br />
          <span className="out">Backend Dev.</span>
        </h1>
        <p className="hero-desc">
          Estudiante de programación especializado en backend con{' '}
          <strong style={{ color: 'var(--text)' }}>Java &amp; Spring</strong>.
          Construyo APIs, arquitecturas monolíticas y empiezo a explorar
          el mundo de los microservicios.
        </p>
        <div className="ctas">
          <a href="#projects" className="btn-p">Ver proyectos</a>
          <a href="#contact" className="btn-s">Contáctame</a>
        </div>
      </div>

      {/* Foto derecha */}
      <div className="hero-photo-wrap">
        <div style={{ position: 'relative', display: 'inline-block', width: '100%' }}>
          <div className="photo-corner tl"></div>
          <div className="photo-corner br"></div>
          <div className="photo-frame">

            {!imgError && (
              <img
                src="/img/Avatar.png"
                alt="Adrián Garcés"
                id="profile-img"
                onError={() => setImgError(true)}
              />
            )}

            {imgError && (
              <div className="photo-placeholder" id="placeholder" style={{ display: 'flex' }}>
                <svg viewBox="0 0 64 64" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="32" cy="22" r="12" />
                  <path d="M8 56c0-13.255 10.745-24 24-24s24 10.745 24 24" />
                </svg>
                <span>Sin foto de perfil</span>
              </div>
            )}

          </div>
        </div>
        {/* Chips bajo la foto */}
        <div className="photo-chips">
          <span className="chip active">Open to work</span>
          <span className="chip">Java Dev</span>
          <span className="chip">Backend</span>
        </div>
      </div>
    </section>
  )
}
