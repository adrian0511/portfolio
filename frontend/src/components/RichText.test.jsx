import { describe, it, expect } from 'vitest'
import { render } from '@testing-library/react'
import RichText from './RichText.jsx'

describe('RichText', () => {
  it('convierte los segmentos **...** en <strong>', () => {
    const { container } = render(<RichText>Hola **mundo** cruel</RichText>)

    const strongs = container.querySelectorAll('strong')
    expect(strongs).toHaveLength(1)
    expect(strongs[0].textContent).toBe('mundo')
    expect(container.textContent).toBe('Hola mundo cruel')
  })

  it('soporta varios segmentos en negrita en el mismo texto', () => {
    const { container } = render(<RichText>**Uno** y **dos**</RichText>)

    const strongs = container.querySelectorAll('strong')
    expect(strongs).toHaveLength(2)
    expect(strongs[0].textContent).toBe('Uno')
    expect(strongs[1].textContent).toBe('dos')
  })

  it('sin negritas, devuelve el texto plano sin <strong>', () => {
    const { container } = render(<RichText>texto sin formato</RichText>)

    expect(container.querySelector('strong')).toBeNull()
    expect(container.textContent).toBe('texto sin formato')
  })
})
