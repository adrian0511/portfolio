import { useEffect, useRef } from 'react'

// Cursor personalizado (punto + anillo con lag). Port fiel del IIFE del script.js:
// - solo en dispositivos con mouse fino (hover:hover + pointer:fine)
// - aparece tras el primer mousemove (.vis)
// - el punto sigue al instante; el anillo con inercia (0.14)
// - al hover sobre a/button el punto se agranda como aro
export default function CustomCursor() {
  const dotRef = useRef(null)
  const ringRef = useRef(null)

  useEffect(() => {
    const dot = dotRef.current
    const ring = ringRef.current

    const hasRealMouse = window.matchMedia('(hover: hover) and (pointer: fine)').matches
    if (!hasRealMouse) return // tablets/moviles: no cursor custom

    let mx = 0, my = 0, rx = 0, ry = 0
    let started = false
    let raf = 0

    const onMove = (e) => {
      mx = e.clientX
      my = e.clientY
      if (!started) {
        rx = mx
        ry = my
        dot.classList.add('vis')
        ring.classList.add('vis')
        started = true
      }
    }

    const loop = () => {
      if (started) {
        dot.style.transform = `translate(${mx - 4.5}px, ${my - 4.5}px)`
        rx += (mx - rx) * 0.14
        ry += (my - ry) * 0.14
        ring.style.transform = `translate(${rx - 17}px, ${ry - 17}px)`
      }
      raf = requestAnimationFrame(loop)
    }

    const enter = () => {
      dot.style.width = '26px'
      dot.style.height = '26px'
      dot.style.background = 'transparent'
      dot.style.border = '1.5px solid var(--accent)'
      dot.style.borderRadius = '50%'
    }
    const leave = () => {
      dot.style.width = '9px'
      dot.style.height = '9px'
      dot.style.background = 'var(--accent)'
      dot.style.border = 'none'
    }

    document.addEventListener('mousemove', onMove)
    raf = requestAnimationFrame(loop)

    const interactive = document.querySelectorAll('a, button')
    interactive.forEach((el) => {
      el.addEventListener('mouseenter', enter)
      el.addEventListener('mouseleave', leave)
    })

    return () => {
      document.removeEventListener('mousemove', onMove)
      cancelAnimationFrame(raf)
      interactive.forEach((el) => {
        el.removeEventListener('mouseenter', enter)
        el.removeEventListener('mouseleave', leave)
      })
    }
  }, [])

  return (
    <>
      <div id="c-dot" ref={dotRef}></div>
      <div id="c-ring" ref={ringRef}></div>
    </>
  )
}
