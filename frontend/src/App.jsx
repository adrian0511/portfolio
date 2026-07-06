import { useEffect, useState } from 'react'
import CustomCursor from './components/CustomCursor.jsx'
import MobileDrawer from './components/MobileDrawer.jsx'
import Navbar from './components/Navbar.jsx'
import Hero from './components/Hero.jsx'
import About from './components/About.jsx'
import Projects from './components/Projects.jsx'
import Contact from './components/Contact.jsx'
import Footer from './components/Footer.jsx'
import useRevealOnScroll from './hooks/useRevealOnScroll.js'

// Pasos 1-4 completos: estructura + comportamientos (cursor, reveal, nav solid, drawer).
// Pendiente (pasos 5-6): data + CSRF de /api/projects. A validar en conjunto.
export default function App() {
  const [drawerOpen, setDrawerOpen] = useState(false)

  // Reveal-on-scroll para todos los .rv al montar.
  useRevealOnScroll()

  // Drawer abierto: bloquea scroll del body y permite cerrar con Escape
  // (equivalente al closeDrawer() + keydown del script.js original).
  useEffect(() => {
    document.body.style.overflow = drawerOpen ? 'hidden' : ''
    const onKey = (e) => {
      if (e.key === 'Escape') setDrawerOpen(false)
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [drawerOpen])

  return (
    <>
      <CustomCursor />
      <MobileDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
      <Navbar drawerOpen={drawerOpen} onToggleDrawer={() => setDrawerOpen((v) => !v)} />
      <Hero />
      <About />
      <Projects />
      <Contact />
      <Footer />
    </>
  )
}
