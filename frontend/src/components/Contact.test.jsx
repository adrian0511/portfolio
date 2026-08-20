import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LanguageProvider } from '../i18n/LanguageContext.jsx'
import Contact from './Contact.jsx'

function renderContact(lang) {
  localStorage.setItem('lang', lang)
  return render(
    <LanguageProvider>
      <Contact />
    </LanguageProvider>
  )
}

describe('Contact', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('el enlace de descarga del CV apunta al PDF en español cuando lang=es', () => {
    renderContact('es')

    const link = screen.getByRole('link', { name: /cv/i })
    expect(link).toHaveAttribute('href', '/docs/CV_Adrian_Garces_ES.pdf')
    expect(link).toHaveAttribute('download', 'CV_Adrian_Garces_ES.pdf')
  })

  it('el enlace de descarga del CV apunta al PDF en ingles cuando lang=en', () => {
    renderContact('en')

    const link = screen.getByRole('link', { name: /cv/i })
    expect(link).toHaveAttribute('href', '/docs/CV_Adrian_Garces_EN.pdf')
    expect(link).toHaveAttribute('download', 'CV_Adrian_Garces_EN.pdf')
  })

  it('incluye enlaces a email, GitHub y LinkedIn', () => {
    renderContact('es')

    expect(screen.getByRole('link', { name: /email/i })).toHaveAttribute(
      'href',
      'mailto:adriangarces0310@gmail.com'
    )
    expect(screen.getByRole('link', { name: /github/i })).toHaveAttribute(
      'href',
      'https://github.com/adrian0511'
    )
    expect(screen.getByRole('link', { name: /linkedin/i })).toHaveAttribute(
      'href',
      'https://linkedin.com/in/adrdev'
    )
  })
})
