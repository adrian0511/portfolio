import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render } from '@testing-library/react'
import useRevealOnScroll from './useRevealOnScroll.js'

function TestComponent() {
  useRevealOnScroll()
  return (
    <div>
      <div className="rv" data-testid="item">contenido</div>
    </div>
  )
}

describe('useRevealOnScroll', () => {
  let observedElements
  let triggerIntersect

  beforeEach(() => {
    observedElements = []
    global.IntersectionObserver = vi.fn(function (callback) {
      this.observe = (el) => observedElements.push(el)
      this.unobserve = vi.fn()
      this.disconnect = vi.fn()
      triggerIntersect = (el) => callback([{ target: el, isIntersecting: true }])
    })
  })

  it('observa los elementos .rv al montar', () => {
    render(<TestComponent />)

    expect(observedElements).toHaveLength(1)
  })

  it('añade la clase "on" cuando el elemento entra en el viewport', async () => {
    vi.useFakeTimers()
    const { getByTestId } = render(<TestComponent />)
    const el = getByTestId('item')

    triggerIntersect(el)
    await vi.runAllTimersAsync()

    expect(el.classList.contains('on')).toBe(true)
    vi.useRealTimers()
  })

  it('desconecta el observer al desmontar', () => {
    const { unmount } = render(<TestComponent />)
    const instance = IntersectionObserver.mock.instances[0]

    unmount()

    expect(instance.disconnect).toHaveBeenCalled()
  })
})
