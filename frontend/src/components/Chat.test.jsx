import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LanguageProvider } from '../i18n/LanguageContext.jsx'
import Chat from './Chat.jsx'

vi.mock('../hooks/useChat.js', () => ({
  default: () => ({ messages: [], sending: false, send: vi.fn() }),
}))

function renderChat(props) {
  return render(
    <LanguageProvider>
      <Chat open={false} onOpenChange={() => {}} {...props} />
    </LanguageProvider>
  )
}

describe('Chat', () => {
  it('cerrado muestra solo el lanzador', () => {
    renderChat()

    expect(screen.getByRole('button', { name: /abrir el chat|open the chat/i })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  // Lo controla App: el menu movil abre el chat sin pasar por el lanzador.
  it('se abre desde fuera con la prop open', () => {
    renderChat({ open: true })

    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('se esconde el lanzador mientras el menu movil esta abierto', () => {
    renderChat({ hidden: true })

    expect(document.querySelector('.chat-launcher')).toHaveClass('hidden')
  })

  it('el lanzador pide a App que lo abra', async () => {
    const onOpenChange = vi.fn()
    renderChat({ onOpenChange })

    await userEvent.click(screen.getByRole('button', { name: /abrir el chat|open the chat/i }))

    expect(onOpenChange).toHaveBeenCalledWith(true)
  })
})
