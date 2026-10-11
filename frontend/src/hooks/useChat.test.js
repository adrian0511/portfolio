import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import useChat from './useChat.js'
import * as client from '../api/client.js'

const t = { chat: { error: 'ERROR_MSG', limit: 'LIMIT_MSG' } }

describe('useChat', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(client, 'ensureCsrfCookie').mockResolvedValue('token-123')
  })

  it('añade el turno del usuario y va rellenando el del asistente', async () => {
    vi.spyOn(client, 'streamChat').mockImplementation(async ({ onChunk }) => {
      onChunk('Usa ')
      onChunk('Java.')
    })

    const { result } = renderHook(() => useChat(t))
    await act(() => result.current.send('¿Qué usa?'))

    expect(result.current.messages).toEqual([
      { role: 'user', content: '¿Qué usa?' },
      { role: 'assistant', content: 'Usa Java.' },
    ])
  })

  it('ignora preguntas vacías', async () => {
    const stream = vi.spyOn(client, 'streamChat').mockResolvedValue()

    const { result } = renderHook(() => useChat(t))
    await act(() => result.current.send('   '))

    expect(stream).not.toHaveBeenCalled()
    expect(result.current.messages).toEqual([])
  })

  it('reutiliza el token CSRF entre preguntas', async () => {
    vi.spyOn(client, 'streamChat').mockImplementation(async ({ onChunk }) => onChunk('ok'))

    const { result } = renderHook(() => useChat(t))
    await act(() => result.current.send('una'))
    await act(() => result.current.send('otra'))

    expect(client.ensureCsrfCookie).toHaveBeenCalledTimes(1)
  })

  // Cerrar el panel a mitad de respuesta tenia que parar el fetch, y el aborto
  // no es un fallo que contarle al visitante: si se pintara, el turno se
  // quedaria con un "no puedo responder" pegado al final.
  it('cancel aborta el stream en vuelo sin dejar mensaje de error', async () => {
    let signal
    vi.spyOn(client, 'streamChat').mockImplementation(
      (args) =>
        new Promise((_resolve, reject) => {
          signal = args.signal
          args.onChunk('a medias')
          args.signal.addEventListener('abort', () => {
            const err = new Error('aborted')
            err.name = 'AbortError'
            reject(err)
          })
        })
    )

    const { result } = renderHook(() => useChat(t))
    act(() => {
      result.current.send('hola')
    })
    await waitFor(() => expect(signal).toBeInstanceOf(AbortSignal))

    await act(async () => result.current.cancel())

    expect(signal.aborted).toBe(true)
    expect(result.current.messages.at(-1).content).toBe('a medias')
    expect(result.current.sending).toBe(false)
  })

  it('muestra el mensaje de límite cuando el backend responde 429', async () => {
    vi.spyOn(client, 'streamChat').mockRejectedValue(new Error('chat: rate-limit'))
    vi.spyOn(console, 'error').mockImplementation(() => {})

    const { result } = renderHook(() => useChat(t))
    await act(() => result.current.send('hola'))

    await waitFor(() => expect(result.current.messages[1].content).toBe('LIMIT_MSG'))
  })

  it('ante cualquier otro fallo muestra el mensaje de error, no rompe', async () => {
    vi.spyOn(client, 'streamChat').mockRejectedValue(new Error('chat: HTTP 500'))
    vi.spyOn(console, 'error').mockImplementation(() => {})

    const { result } = renderHook(() => useChat(t))
    await act(() => result.current.send('hola'))

    await waitFor(() => expect(result.current.messages[1].content).toBe('ERROR_MSG'))
  })
})
