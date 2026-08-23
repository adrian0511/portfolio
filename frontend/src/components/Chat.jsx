import { useEffect, useRef, useState } from 'react'
import { FiMessageSquare, FiX, FiSend } from 'react-icons/fi'
import { useLanguage } from '../i18n/LanguageContext.jsx'
import useChat from '../hooks/useChat.js'
import ChatText from './ChatText.jsx'

export default function Chat() {
  const { t } = useLanguage()
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState('')
  const { messages, sending, send } = useChat(t)
  const logRef = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('keydown', onKey)
    inputRef.current?.focus()
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  // Seguir el texto según se genera, sin que el usuario tenga que desplazarse.
  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight
  }, [messages])

  const submit = (e) => {
    e.preventDefault()
    send(draft)
    setDraft('')
  }

  return (
    <>
      <button
        className={`chat-launcher${open ? ' hidden' : ''}`}
        onClick={() => setOpen(true)}
        aria-label={t.chat.open}
      >
        <FiMessageSquare aria-hidden="true" />
        <span>{t.chat.launcher}</span>
      </button>

      {open && (
        <section className="chat-panel" role="dialog" aria-label={t.chat.title}>
          <header className="chat-head">
            <div>
              <strong>{t.chat.title}</strong>
              <span className="chat-badge">{t.chat.badge}</span>
            </div>
            <button onClick={() => setOpen(false)} aria-label={t.chat.close}>
              <FiX aria-hidden="true" />
            </button>
          </header>

          <div className="chat-log" ref={logRef}>
            {messages.length === 0 && (
              <div className="chat-empty">
                <p>{t.chat.intro}</p>
                <ul>
                  {t.chat.samples.map((s) => (
                    <li key={s}>
                      <button type="button" onClick={() => send(s)}>{s}</button>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {messages.map((m, i) => (
              <div key={i} className={`chat-msg ${m.role}`}>
                {m.content
                  ? (m.role === 'assistant' ? <ChatText>{m.content}</ChatText> : m.content)
                  : <span className="chat-typing" aria-label={t.chat.thinking}>···</span>}
              </div>
            ))}
          </div>

          <form className="chat-form" onSubmit={submit}>
            <input
              ref={inputRef}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder={t.chat.placeholder}
              maxLength={500}
              disabled={sending}
            />
            <button type="submit" disabled={sending || !draft.trim()} aria-label={t.chat.send}>
              <FiSend aria-hidden="true" />
            </button>
          </form>
        </section>
      )}
    </>
  )
}
