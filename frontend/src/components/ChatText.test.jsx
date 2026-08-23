import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import ChatText from './ChatText.jsx'

describe('ChatText', () => {
  it('convierte **texto** en negrita en lugar de mostrar los asteriscos', () => {
    const { container } = render(<ChatText>{'Domina **Java** y **Spring**.'}</ChatText>)

    expect(container.textContent).not.toContain('*')
    expect([...container.querySelectorAll('strong')].map((s) => s.textContent))
      .toEqual(['Java', 'Spring'])
  })

  it('convierte las viñetas del modelo en una lista', () => {
    const text = 'Sus proyectos:\n\n*   **prompt-link**: librería Java.\n*   **cookbook**: recetas.'
    const { container } = render(<ChatText>{text}</ChatText>)

    const items = container.querySelectorAll('.chat-list li')
    expect(items).toHaveLength(2)
    expect(items[0].textContent).toBe('prompt-link: librería Java.')
    expect(container.textContent).not.toContain('*')
  })

  it('acepta guiones y topos como marcadores de viñeta', () => {
    const { container } = render(<ChatText>{'- uno\n- dos\n• tres'}</ChatText>)

    expect(container.querySelectorAll('.chat-list li')).toHaveLength(3)
  })

  it('separa los párrafos de las listas', () => {
    const { container } = render(<ChatText>{'Intro.\n\n- uno\n\nCierre.'}</ChatText>)

    expect(container.querySelectorAll('p')).toHaveLength(2)
    expect(container.querySelectorAll('.chat-list')).toHaveLength(1)
  })

  it('oculta un ** sin cerrar mientras el texto sigue llegando', () => {
    // Durante el streaming el cierre aún no ha llegado; mostrarlo haría
    // parpadear los asteriscos en pantalla.
    const { container } = render(<ChatText>{'Domina **Ja'}</ChatText>)

    expect(container.textContent).toBe('Domina')
    expect(container.textContent).not.toContain('*')
  })

  it('ignora un marcador de viñeta cuyo texto aún no ha llegado', () => {
    // Durante el streaming el "-" llega antes que su contenido.
    const { container } = render(<ChatText>{'Proyectos:\n\n- uno\n-'}</ChatText>)

    expect(container.querySelectorAll('.chat-list li')).toHaveLength(1)
    expect(container.textContent).toBe('Proyectos:uno')
  })

  it('un texto plano se renderiza tal cual', () => {
    const { container } = render(<ChatText>{'No consta en su perfil.'}</ChatText>)

    expect(container.textContent).toBe('No consta en su perfil.')
  })
})
