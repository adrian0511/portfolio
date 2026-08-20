import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import useProjects from './useProjects.js'
import * as client from '../api/client.js'

describe('useProjects', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('empieza en loading y pasa a success con los repos del backend', async () => {
    vi.spyOn(client, 'getCsrfToken').mockResolvedValue('token-123')
    vi.spyOn(client, 'getProjects').mockResolvedValue([{ name: 'demo' }])

    const { result } = renderHook(() => useProjects())

    expect(result.current.status).toBe('loading')

    await waitFor(() => expect(result.current.status).toBe('success'))
    expect(result.current.repos).toEqual([{ name: 'demo' }])
    expect(client.getProjects).toHaveBeenCalledWith('token-123')
  })

  it('pasa a error si el flujo csrf-token/projects falla', async () => {
    vi.spyOn(client, 'getCsrfToken').mockRejectedValue(new Error('csrf-token: HTTP 500'))
    vi.spyOn(client, 'getProjects')
    vi.spyOn(console, 'error').mockImplementation(() => {})

    const { result } = renderHook(() => useProjects())

    await waitFor(() => expect(result.current.status).toBe('error'))
    expect(result.current.repos).toEqual([])
    expect(client.getProjects).not.toHaveBeenCalled()
  })
})
