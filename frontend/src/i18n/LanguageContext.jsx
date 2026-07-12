import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { translations } from './translations.js'

const LanguageContext = createContext(null)
const STORAGE_KEY = 'lang'
const SUPPORTED = ['es', 'en']
const DEFAULT = 'en' // inglés como default internacional

// Idioma inicial:
//   1) la elección explícita del usuario (localStorage) — siempre manda
//   2) el idioma del navegador (navigator.language): si es español -> es
//   3) inglés por defecto
function detectLanguage() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved && SUPPORTED.includes(saved)) return saved
  } catch {
    // localStorage puede fallar (modo privado / cookies bloqueadas): seguimos con la detección
  }

  const browser = navigator.languages?.[0] || navigator.language || ''
  return browser.toLowerCase().startsWith('es') ? 'es' : DEFAULT
}

export function LanguageProvider({ children }) {
  const [lang, setLangState] = useState(detectLanguage)

  // Mantiene <html lang> sincronizado (accesibilidad y SEO)
  useEffect(() => {
    document.documentElement.lang = lang
  }, [lang])

  const setLang = (next) => {
    if (!SUPPORTED.includes(next)) return
    setLangState(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      // si no se puede persistir, el idioma igual funciona en esta sesión
    }
  }

  const value = useMemo(() => ({ lang, setLang, t: translations[lang] }), [lang])

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export function useLanguage() {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useLanguage debe usarse dentro de <LanguageProvider>')
  return ctx
}
