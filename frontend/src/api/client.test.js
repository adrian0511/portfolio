import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getCsrfToken, getProjects } from './client.js'

describe('getCsrfToken', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('pide /api/csrf-token con credentials include y devuelve el token', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ token: 'abc-123' }),
    })

    const token = await getCsrfToken()

    expect(token).toBe('abc-123')
    expect(fetch).toHaveBeenCalledWith('/api/csrf-token', { credentials: 'include' })
  })

  it('lanza un error si la respuesta no es ok', async () => {
    global.fetch = vi.fn().mockResolvedValue({ ok: false, status: 500 })

    await expect(getCsrfToken()).rejects.toThrow('csrf-token: HTTP 500')
  })
})

describe('getProjects', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('envia el token como header X-CSRF-Token y devuelve la lista', async () => {
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
      headers: { 'X-CSRF-Token': 'mi-token' },
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
