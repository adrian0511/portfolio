import { useLanguage } from '../i18n/LanguageContext.jsx'

// Seccion Contacto. El CV se descarga en el idioma en que esté la web
// (el toggle de banderas cambia cuál se baja).
export default function Contact() {
  const { lang, t } = useLanguage()

  const cvFile = `/docs/CV_Adrian_Garces_${lang.toUpperCase()}.pdf`
  const cvName = `CV_Adrian_Garces_${lang.toUpperCase()}.pdf`

  return (
    <section id="contact">
      <div className="cglow"></div>
      <div className="sec-lbl rv">{t.contact.label}</div>
      <div className="cbig rv">
        {t.contact.heading[0]}<br /><span>{t.contact.heading[1]}</span>
      </div>
      <p className="csub rv">{t.contact.sub}</p>
      <div className="slinks rv">
        <a href="mailto:adriangarces0310@gmail.com" className="sl">✉ Email</a>
        <a href="https://github.com/adrian0511" className="sl" target="_blank" rel="noreferrer">⌥ GitHub</a>
        <a href="https://linkedin.com/in/adrdev" className="sl" target="_blank" rel="noreferrer">in LinkedIn</a>
        <a href={cvFile} download={cvName} className="sl">{t.contact.cv}</a>
      </div>
    </section>
  )
}
