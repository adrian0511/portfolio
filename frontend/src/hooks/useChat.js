import { useCallback, useRef, useState } from 'react'
import { getCsrfToken, streamChat } from '../api/client.js'

const MAX_HISTORY = 6

export default function useChat(t) {
  const [messages, setMessages] = useState([])
  const [sending, setSending] = useState(false)
  const tokenRef = useRef(null)

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

      try {
        if (!tokenRef.current) tokenRef.current = await getCsrfToken()

        await streamChat({
          csrfToken: tokenRef.current,
          question: text,
          history: messages.slice(-MAX_HISTORY),
          onChunk: appendToLast,
        })
      } catch (err) {
        console.error('Chat:', err)
        appendToLast(err.message === 'chat: rate-limit' ? t.chat.limit : t.chat.error)
      } finally {
        setSending(false)
      }
    },
    [messages, sending, t]
  )

  return { messages, sending, send }
}
