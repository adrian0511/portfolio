import { useEffect } from 'react'

// Reemplaza el IntersectionObserver del script.js original:
// revela los elementos .rv (opacity 0 -> 1, translateY) al entrar en viewport,
// con un stagger de 75ms. Se re-escanea segun `deps` (util cuando aparecen
// tarjetas nuevas, p.ej. al cargar proyectos en el paso 5).
export default function useRevealOnScroll(deps = []) {
  useEffect(() => {
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry, i) => {
          if (entry.isIntersecting) {
            setTimeout(() => entry.target.classList.add('on'), i * 75)
            io.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.07, rootMargin: '0px 0px -28px 0px' }
    )

    document.querySelectorAll('.rv:not(.on)').forEach((el) => io.observe(el))
    return () => io.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}
