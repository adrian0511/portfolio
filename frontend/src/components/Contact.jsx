// Seccion Contacto. Sin .rv (reveal) todavia: se agrega en el paso 4.
export default function Contact() {
  return (
    <section id="contact">
      <div className="cglow"></div>
      <div className="sec-lbl rv">03 — Contacto</div>
      <div className="cbig rv">Trabajemos<br /><span>juntos.</span></div>
      <p className="csub rv">¿Tienes un proyecto backend interesante o quieres hablar de arquitectura de software?<br />Mi inbox
        siempre está abierto.</p>
      <div className="slinks rv">
        <a href="mailto:adriangarces0310@gmail.com" className="sl">✉ Email</a>
        <a href="https://github.com/adrian0511" className="sl" target="_blank" rel="noreferrer">⌥ GitHub</a>
        <a href="https://linkedin.com/in/adrdev" className="sl" target="_blank" rel="noreferrer">in LinkedIn</a>
        <a href="/docs/CV_Adrian.pdf" download="CV_Adrian_Garces.pdf" className="sl">↓ Descargar CV</a>
      </div>
    </section>
  )
}
