import { useEffect, useRef } from 'react'

// Nav fijo que se vuelve "solid" tras 40px de scroll (equivalente al listener
// scroll del script.js). La hamburguesa abre/cierra el drawer (estado en App).
export default function Navbar({ drawerOpen, onToggleDrawer }) {
  const navRef = useRef(null)

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
      <ul className="nav-links">
        <li><a href="#about">Sobre mí</a></li>
        <li><a href="#projects">Proyectos</a></li>
        <li><a href="#contact">Contacto</a></li>
      </ul>
      <button
        className={`hbg${drawerOpen ? ' x' : ''}`}
        id="hbg"
        aria-label="Menú"
        aria-expanded={drawerOpen}
        onClick={onToggleDrawer}
      >
        <span></span><span></span><span></span>
      </button>
    </nav>
  )
}
