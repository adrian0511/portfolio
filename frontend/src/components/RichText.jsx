import { Fragment } from 'react'

// Construye nodos React en vez de usar dangerouslySetInnerHTML: el texto sale de
// las traducciones hoy, pero un <strong> no justifica abrir esa puerta.
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
