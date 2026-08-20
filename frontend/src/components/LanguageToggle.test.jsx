import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LanguageProvider, useLanguage } from '../i18n/LanguageContext.jsx'
import LanguageToggle from './LanguageToggle.jsx'

function CurrentLang() {
  const { lang } = useLanguage()
  return <span data-testid="current-lang">{lang}</span>
}

function renderToggle() {
  return render(
    <LanguageProvider>
      <LanguageToggle />
      <CurrentLang />
    </LanguageProvider>
  )
}

describe('LanguageToggle', () => {
  beforeEach(() => {
    localStorage.clear()
    Object.defineProperty(window.navigator, 'language', { value: 'en-US', configurable: true })
  })

  it('marca como activo el boton del idioma actual', () => {
    renderToggle()

    expect(screen.getByRole('button', { name: /^en$/i })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: /^es$/i })).toHaveAttribute('aria-pressed', 'false')
  })

  it('cambia el idioma al pulsar ES', async () => {
    const user = userEvent.setup()
    renderToggle()

    await user.click(screen.getByRole('button', { name: /^es$/i }))

    expect(screen.getByTestId('current-lang')).toHaveTextContent('es')
    expect(screen.getByRole('button', { name: /^es$/i })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: /^en$/i })).toHaveAttribute('aria-pressed', 'false')
  })
})
