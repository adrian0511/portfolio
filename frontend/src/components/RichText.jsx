import { Fragment } from 'react'

// Convierte los **...** de las traducciones en <strong>, construyendo nodos React.
// No usa innerHTML/dangerouslySetInnerHTML, así que no hay riesgo de inyección.
export default function RichText({ children }) {
  const parts = String(children).split(/(\*\*[^*]+\*\*)/g)

  return parts.map((part, i) =>
    part.startsWith('**') && part.endsWith('**') ? (
      <strong key={i}>{part.slice(2, -2)}</strong>
    ) : (
      <Fragment key={i}>{part}</Fragment>
    )
  )
}
