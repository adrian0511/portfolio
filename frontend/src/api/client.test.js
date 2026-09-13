import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ensureCsrfCookie, getProjects } from './client.js'

describe('getProjects', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('envia el token como header X-XSRF-TOKEN y devuelve la lista', async () => {
    const repos = [{ name: 'demo', html_url: 'https://github.com/adrian0511/demo' }]
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(repos),
    })

    const result = await getProjects('mi-token')

    expect(result).toEqual(repos)
    expect(fetch).toHaveBeenCalledWith('/api/projects', {
      credentials: 'include',
      headers: { 'X-XSRF-TOKEN': 'mi-token' },
    })
  })

  it('devuelve una lista vacia cuando el backend responde 204', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, status: 204 })

    const result = await getProjects('mi-token')

    expect(result).toEqual([])
  })

  it('lanza un error si el backend responde 404 (CSRF invalido)', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 404 })

    await expect(getProjects('token-invalido')).rejects.toThrow('projects: HTTP 404')
  })
})

describe('ensureCsrfCookie', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    document.cookie = 'XSRF-TOKEN=; max-age=0'
  })

  it('devuelve el token de la cookie sin pedir nada al backend', async () => {
    document.cookie = 'XSRF-TOKEN=cookie-123'
    global.fetch = vi.fn()

    await expect(ensureCsrfCookie()).resolves.toBe('cookie-123')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('provoca una respuesta del backend cuando la cookie aun no esta', async () => {
    // En dev el index.html lo sirve Vite, asi que puede no haber pasado por el
    // backend ninguna respuesta que emita la cookie.
    global.fetch = vi.fn().mockImplementation(() => {
      document.cookie = 'XSRF-TOKEN=recien-puesta'
      return Promise.resolve({ ok: true, status: 200 })
    })

    await expect(ensureCsrfCookie()).resolves.toBe('recien-puesta')
    expect(fetch).toHaveBeenCalledWith('/api/csrf-token', { credentials: 'include' })
  })

  it('lanza error si tras llamar al backend sigue sin haber cookie', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: true, status: 200 })

    await expect(ensureCsrfCookie()).rejects.toThrow('csrf: sin cookie')
  })
})
