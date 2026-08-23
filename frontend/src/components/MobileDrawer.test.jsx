import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LanguageProvider } from '../i18n/LanguageContext.jsx'
import MobileDrawer from './MobileDrawer.jsx'

function renderDrawer(lang, props = {}) {
  localStorage.setItem('lang', lang)
  return render(
    <LanguageProvider>
      <MobileDrawer open onClose={() => {}} onOpenChat={() => {}} {...props} />
    </LanguageProvider>
  )
}

describe('MobileDrawer', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('ofrece el chat de IA junto a las secciones', () => {
    renderDrawer('es')

    expect(screen.getByRole('button', { name: /preguntar a la ia/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /proyectos/i })).toBeInTheDocument()
  })

  it('traduce la entrada del chat', () => {
    renderDrawer('en')

    expect(screen.getByRole('button', { name: /ask the ai/i })).toBeInTheDocument()
  })

  // En movil el boton flotante es un icono facil de pasar por alto, asi que el
  // menu es la otra via de entrada al chat.
  it('al pulsar el chat avisa a App para abrirlo', async () => {
    const onOpenChat = vi.fn()
    renderDrawer('es', { onOpenChat })

    await userEvent.click(screen.getByRole('button', { name: /preguntar a la ia/i }))

    expect(onOpenChat).toHaveBeenCalledTimes(1)
  })
})
