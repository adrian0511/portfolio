// El backend valida el token CSRF contra la sesión, así que la cookie SESSION
// debe viajar en ambas llamadas: de ahí el credentials: 'include'. Sin él, el
// token pedido en /api/csrf-token no coincide con ninguna sesión y /api/projects
// responde 404.

export async function getCsrfToken() {
  const res = await fetch('/api/csrf-token', { credentials: 'include' })
  if (!res.ok) throw new Error(`csrf-token: HTTP ${res.status}`)
  const data = await res.json()
  return data.token
}

export async function getProjects(csrfToken) {
  const res = await fetch('/api/projects', {
    credentials: 'include',
    headers: { 'X-CSRF-Token': csrfToken },
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
      'X-CSRF-Token': csrfToken,
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
