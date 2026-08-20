import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { act } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { LanguageProvider, useLanguage } from './LanguageContext.jsx'

function setBrowserLanguage(value) {
  Object.defineProperty(window.navigator, 'language', { value, configurable: true })
  Object.defineProperty(window.navigator, 'languages', { value: [value], configurable: true })
}

describe('LanguageProvider / useLanguage', () => {
  beforeEach(() => {
    localStorage.clear()
    setBrowserLanguage('en-US')
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('usa ingles por defecto si el navegador no es español y no hay preferencia guardada', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper: LanguageProvider })
    expect(result.current.lang).toBe('en')
  })

  it('detecta español a partir del idioma del navegador', () => {
    setBrowserLanguage('es-AR')
    const { result } = renderHook(() => useLanguage(), { wrapper: LanguageProvider })
    expect(result.current.lang).toBe('es')
  })

  it('la preferencia guardada en localStorage tiene prioridad sobre el navegador', () => {
    localStorage.setItem('lang', 'es')
    setBrowserLanguage('en-US')
    const { result } = renderHook(() => useLanguage(), { wrapper: LanguageProvider })
    expect(result.current.lang).toBe('es')
  })

  it('setLang cambia el idioma, lo persiste y actualiza <html lang>', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper: LanguageProvider })

    act(() => result.current.setLang('es'))

    expect(result.current.lang).toBe('es')
    expect(localStorage.getItem('lang')).toBe('es')
    expect(document.documentElement.lang).toBe('es')
  })

  it('setLang ignora idiomas no soportados', () => {
    const { result } = renderHook(() => useLanguage(), { wrapper: LanguageProvider })

    act(() => result.current.setLang('fr'))

    expect(result.current.lang).toBe('en')
    expect(localStorage.getItem('lang')).toBeNull()
  })

  it('useLanguage fuera de LanguageProvider lanza un error', () => {
    expect(() => renderHook(() => useLanguage())).toThrow(
      'useLanguage debe usarse dentro de <LanguageProvider>'
    )
  })
})
