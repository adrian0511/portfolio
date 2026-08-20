import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LanguageProvider } from '../i18n/LanguageContext.jsx'
import Footer from './Footer.jsx'

describe('Footer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('muestra el año actual dinámicamente', () => {
    vi.setSystemTime(new Date('2031-06-15T00:00:00Z'))

    render(
      <LanguageProvider>
        <Footer />
      </LanguageProvider>
    )

    expect(screen.getByText('2031')).toBeInTheDocument()
  })
})
