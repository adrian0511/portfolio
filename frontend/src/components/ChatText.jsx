
import RichText from './RichText.jsx'

const BULLET = /^\s*[*\-•]\s+/

// Mientras llega el stream puede quedar un "**" a medio cerrar; sin esto el
// visitante ve los asteriscos parpadear hasta que llega el cierre.
function hideIncompleteBold(text) {
  const marks = text.match(/\*\*/g)
  if (!marks || marks.length % 2 === 0) return text
  return text.slice(0, text.lastIndexOf('**'))
}

/**
 * El modelo responde en markdown ligero (negritas y viñetas). Se renderiza
 * construyendo nodos React, igual que RichText: nunca con innerHTML.
 */
export default function ChatText({ children }) {
  const blocks = []
  let bullets = []

  const flush = () => {
    if (bullets.length) {
      blocks.push({ type: 'ul', items: bullets })
      bullets = []
    }
  }

  for (const raw of hideIncompleteBold(String(children)).split('\n')) {
    const line = raw.trim()
    if (!line) {
      flush()
      continue
    }

    // Marcador solo: es una viñeta cuyo texto aún no ha llegado por el stream.
    if (/^[*\-•]$/.test(line)) {
      continue
    }

    if (BULLET.test(line)) {
      bullets.push(line.replace(BULLET, ''))
    } else {
      flush()
      blocks.push({ type: 'p', text: line })
    }
  }
  flush()

  return blocks.map((block, i) =>
    block.type === 'ul' ? (
      <ul key={i} className="chat-list">
        {block.items.map((item, j) => (
          <li key={j}><RichText>{item}</RichText></li>
        ))}
      </ul>
    ) : (
      <p key={i}><RichText>{block.text}</RichText></p>
    )
  )
}
