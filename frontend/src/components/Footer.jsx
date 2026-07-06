// Footer con año dinámico (equivalente a new Date().getFullYear() del JS original).
export default function Footer() {
  const year = new Date().getFullYear()
  return (
    <footer>
      <span>© <span id="year">{year}</span> Adrián Garcés — Java &amp; Spring enthusiast ☕</span>
      <span>HTML · CSS · Vanilla JS</span>
    </footer>
  )
}
