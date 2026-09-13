import { useEffect } from 'react'

// `deps` fuerza un re-escaneo: sin eso, los elementos que aparecen después del
// montaje (las tarjetas de proyectos al llegar la respuesta) no se observarían.
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
