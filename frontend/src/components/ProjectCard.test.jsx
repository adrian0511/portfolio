import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LanguageProvider } from '../i18n/LanguageContext.jsx'
import ProjectCard from './ProjectCard.jsx'

function renderCard(repo, lang = 'es') {
  localStorage.setItem('lang', lang)
  return render(
    <LanguageProvider>
      <ProjectCard repo={repo} index={0} />
    </LanguageProvider>
  )
}

const baseRepo = {
  name: 'gym-reservas',
  description: 'Sistema de reservas',
  html_url: 'https://github.com/adrian0511/gym-reservas',
  language: 'TypeScript',
  topics: ['jwt-authentication', 'rest-api', 'nestjs'],
  pushed_at: '2026-08-18T10:00:00Z',
}

describe('ProjectCard', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-08-21T10:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
    localStorage.clear()
  })

  it('convierte el nombre del repo en un título legible', () => {
    renderCard(baseRepo)
    expect(screen.getByRole('heading')).toHaveTextContent('Gym Reservas')
  })

  it('respeta las mayúsculas ya presentes en el nombre', () => {
    renderCard({ ...baseRepo, name: 'RetosConIA' })
    expect(screen.getByRole('heading')).toHaveTextContent('RetosConIA')
  })

  it('muestra el lenguaje y los topics del repo', () => {
    renderCard(baseRepo)

    expect(screen.getByText('TypeScript')).toBeInTheDocument()
    expect(screen.getByText('jwt-authentication')).toBeInTheDocument()
    expect(screen.getByText('rest-api')).toBeInTheDocument()
    expect(screen.getByText('nestjs')).toBeInTheDocument()
  })

  it('usa el logo del lenguaje cuando existe', () => {
    const { container } = renderCard(baseRepo)

    expect(container.querySelector('.plang-icon')).not.toBeNull()
    expect(container.querySelector('.plang-dot')).toBeNull()
  })

  it('cae al punto de color si el lenguaje no tiene logo', () => {
    const { container } = renderCard({ ...baseRepo, language: 'Brainfuck' })

    expect(container.querySelector('.plang-icon')).toBeNull()
    expect(container.querySelector('.plang-dot')).not.toBeNull()
    expect(screen.getByText('Brainfuck')).toBeInTheDocument()
  })

  it('no renderiza chips de topics si el repo no tiene', () => {
    const { container } = renderCard({ ...baseRepo, topics: [] })
    expect(container.querySelector('.ptags')).toBeNull()
  })

  it('muestra cuándo se actualizó por última vez, en el idioma activo', () => {
    renderCard(baseRepo, 'es')
    expect(screen.getByText(/Actualizado hace 3 días/)).toBeInTheDocument()
  })

  it('redondea el tiempo transcurrido en vez de truncarlo', () => {
    // 3 días y 17 horas: truncar daría "hace 3 días", que se lee como un día menos.
    renderCard({ ...baseRepo, pushed_at: '2026-08-17T17:00:00Z' }, 'es')
    expect(screen.getByText(/Actualizado hace 4 días/)).toBeInTheDocument()
  })

  it('localiza el tiempo transcurrido en inglés', () => {
    renderCard(baseRepo, 'en')
    expect(screen.getByText(/Updated 3 days ago/)).toBeInTheDocument()
  })

  it('omite la fecha si el repo no trae pushed_at (lista de respaldo)', () => {
    renderCard({ ...baseRepo, pushed_at: null })
    expect(screen.queryByText(/Actualizado/)).toBeNull()
  })

  it('enlaza al repositorio en GitHub', () => {
    renderCard(baseRepo)
    expect(screen.getByRole('link')).toHaveAttribute('href', baseRepo.html_url)
  })

  it('usa la descripción de respaldo cuando el repo no tiene', () => {
    renderCard({ ...baseRepo, description: null })
    expect(screen.getByText('Proyecto backend con Spring Boot')).toBeInTheDocument()
  })
})
