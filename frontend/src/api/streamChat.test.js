import { describe, it, expect, vi, beforeEach } from 'vitest'
import { streamChat } from './client.js'

// Simula el ReadableStream que devuelve fetch, troceado como llegaría por red.
function bodyOf(...chunks) {
  const encoder = new TextEncoder()
  let i = 0
  return {
    getReader: () => ({
      read: () =>
        Promise.resolve(
          i < chunks.length ? { done: false, value: encoder.encode(chunks[i++]) } : { done: true }
        ),
    }),
  }
}

function mockResponse(body, { ok = true, status = 200 } = {}) {
  global.fetch = vi.fn().mockResolvedValue({ ok, status, body })
}

describe('streamChat', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('reconstruye la respuesta a partir de los eventos SSE', async () => {
    mockResponse(bodyOf('data:Usa \n\n', 'data:Java.\n\n'))
    const chunks = []

    await streamChat({ csrfToken: 't', question: 'q', history: [], onChunk: (c) => chunks.push(c) })

    expect(chunks.join('')).toBe('Usa Java.')
  })

  it('no parte un evento aunque llegue troceado entre lecturas', async () => {
    // La red puede cortar en cualquier byte, incluso a mitad de "data:".
    mockResponse(bodyOf('data:Spri', 'ng Boot\n\n'))
    const chunks = []

    await streamChat({ csrfToken: 't', question: 'q', history: [], onChunk: (c) => chunks.push(c) })

    expect(chunks.join('')).toBe('Spring Boot')
  })

  it('une las lineas data: de un mismo evento con salto de linea', async () => {
    mockResponse(bodyOf('data:linea uno\ndata:linea dos\n\n'))
    const chunks = []

    await streamChat({ csrfToken: 't', question: 'q', history: [], onChunk: (c) => chunks.push(c) })

    expect(chunks.join('')).toBe('linea uno\nlinea dos')
  })

  it('envia el token CSRF y la pregunta al backend', async () => {
    mockResponse(bodyOf('data:ok\n\n'))

    await streamChat({ csrfToken: 'abc', question: '¿Java?', history: [], onChunk: () => {} })

    const [url, options] = fetch.mock.calls[0]
    expect(url).toBe('/api/chat')
    expect(options.method).toBe('POST')
    expect(options.credentials).toBe('include')
    expect(options.headers['X-CSRF-Token']).toBe('abc')
    expect(JSON.parse(options.body).question).toBe('¿Java?')
  })

  it('distingue el limite de sesion del resto de errores', async () => {
    mockResponse(null, { ok: false, status: 429 })

    await expect(
      streamChat({ csrfToken: 't', question: 'q', history: [], onChunk: () => {} })
    ).rejects.toThrow('chat: rate-limit')
  })

  it('lanza error si el backend responde mal', async () => {
    mockResponse(null, { ok: false, status: 500 })

    await expect(
      streamChat({ csrfToken: 't', question: 'q', history: [], onChunk: () => {} })
    ).rejects.toThrow('chat: HTTP 500')
  })
})
