import { useEffect, useState } from 'react'
import { getCsrfToken, getProjects } from '../api/client.js'

// Reproduce el flujo del script.js: pide el token CSRF, luego los proyectos.
// Ante cualquier error (token o proyectos), pasa a estado 'error' y el
// componente muestra el fallback. Nota: el backend ya tiene su propio fallback
// (repos hardcodeados si GitHub falla), asi que normalmente devuelve datos.
export default function useProjects() {
  const [status, setStatus] = useState('loading') // 'loading' | 'success' | 'error'
  const [repos, setRepos] = useState([])

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      try {
        const token = await getCsrfToken()
        const data = await getProjects(token)
        if (cancelled) return
        setRepos(data)
        setStatus('success')
      } catch (err) {
        if (cancelled) return
        console.error('No se pudieron cargar los proyectos:', err)
        setStatus('error')
      }
    })()

    return () => {
      cancelled = true
    }
  }, [])

  return { status, repos }
}
