import { useEffect, useState } from 'react'
import { getCsrfToken, getProjects } from '../api/client.js'

// El backend ya devuelve una lista de respaldo si GitHub falla, así que el
// estado 'error' solo se alcanza si la propia API no responde.
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
