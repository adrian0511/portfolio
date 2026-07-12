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
