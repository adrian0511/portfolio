// Cliente de la API del backend Spring Boot.
//
// Flujo CSRF (igual que el script.js original):
//   1. GET /api/csrf-token  -> crea/lee la sesion y devuelve { token }.
//                              El backend setea la cookie de sesion (SESSION).
//   2. GET /api/projects    -> requiere el header X-CSRF-Token con ese token;
//                              el CsrfValidationFilter lo compara contra la sesion.
//
// credentials: 'include' garantiza que la cookie de sesion viaje en ambas
// llamadas (imprescindible para que el token valide). En dev, el proxy de Vite
// (/api -> :8080) hace que todo sea same-origin y la cookie se preserve.

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
