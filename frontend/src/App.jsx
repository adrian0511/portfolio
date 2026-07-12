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

export default function App() {
  const [drawerOpen, setDrawerOpen] = useState(false)

  useRevealOnScroll()

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
