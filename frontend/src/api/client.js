// Un único token para las dos llamadas protegidas: el del CSRF de Spring
// Security, que viaja en la cookie XSRF-TOKEN (legible por JS) y se devuelve en
// la cabecera X-XSRF-TOKEN. De ahí el credentials: 'include' — sin la cookie, el
// backend no tiene contra qué comparar y responde 404 (projects) o 403 (chat).
const CSRF_COOKIE = 'XSRF-TOKEN'

function readCookie(name) {
  const prefix = `${name}=`
  const match = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return match ? match.slice(prefix.length) : null
}

// La cookie la emite cualquier respuesta a /api/**, y /api/csrf-token existe
// justo para provocar una cuando aún no la hay: al cargar la página, o en dev,
// donde el index.html lo sirve Vite y no pasa por el backend.
export async function ensureCsrfCookie() {
  const existing = readCookie(CSRF_COOKIE)
  if (existing) return existing

  await fetch('/api/csrf-token', { credentials: 'include' })
  const token = readCookie(CSRF_COOKIE)
  if (!token) throw new Error('csrf: sin cookie')
  return token
}

export async function getProjects(csrfToken) {
  const res = await fetch('/api/projects', {
    credentials: 'include',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  })
  if (res.status === 204) return [] // noContent -> sin proyectos
  if (!res.ok) throw new Error(`projects: HTTP ${res.status}`)
  return res.json()
}

// El chat va por POST, así que no sirve EventSource (que solo hace GET): hay
// que leer el ReadableStream y parsear los eventos SSE a mano.
export async function streamChat({ csrfToken, question, history, onChunk, signal }) {
  const res = await fetch('/api/chat', {
    method: 'POST',
    credentials: 'include',
    signal,
    headers: {
      'X-XSRF-TOKEN': csrfToken,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ question, history }),
  })

  if (res.status === 429) throw new Error('chat: rate-limit')
  if (!res.ok) throw new Error(`chat: HTTP ${res.status}`)

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // Un evento SSE termina en línea en blanco; lo que quede se procesa luego.
    const events = buffer.split('\n\n')
    buffer = events.pop()

    for (const event of events) {
      const text = event
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5))
        .join('\n')
      if (text) onChunk(text)
    }
  }
}
