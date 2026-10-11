import { useCallback, useEffect, useRef, useState } from 'react'
import { ensureCsrfCookie, streamChat } from '../api/client.js'

const MAX_HISTORY = 6

export default function useChat(t) {
  const [messages, setMessages] = useState([])
  const [sending, setSending] = useState(false)
  const tokenRef = useRef(null)
  const abortRef = useRef(null)

  // Cerrar el panel (o desmontar) no paraba el fetch: seguia llegando el stream
  // y seguia escribiendo en un estado que ya nadie mira.
  const cancel = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
  }, [])

  useEffect(() => cancel, [cancel])

  const send = useCallback(
    async (question) => {
      const text = question.trim()
      if (!text || sending) return

      setSending(true)
      // El turno del asistente entra vacío y se va rellenando con el stream.
      setMessages((prev) => [...prev, { role: 'user', content: text }, { role: 'assistant', content: '' }])

      const appendToLast = (chunk) =>
        setMessages((prev) => {
          const next = [...prev]
          next[next.length - 1] = {
            ...next[next.length - 1],
            content: next[next.length - 1].content + chunk,
          }
          return next
        })

      const controller = new AbortController()
      abortRef.current = controller

      try {
        if (!tokenRef.current) tokenRef.current = await ensureCsrfCookie()

        await streamChat({
          csrfToken: tokenRef.current,
          question: text,
          history: messages.slice(-MAX_HISTORY),
          onChunk: appendToLast,
          signal: controller.signal,
        })
      } catch (err) {
        // Abortar es una decisión del visitante, no un fallo que contarle.
        if (err.name === 'AbortError') return
        console.error('Chat:', err)
        appendToLast(err.message === 'chat: rate-limit' ? t.chat.limit : t.chat.error)
      } finally {
        if (abortRef.current === controller) abortRef.current = null
        setSending(false)
      }
    },
    [messages, sending, t]
  )

  return { messages, sending, send, cancel }
}
