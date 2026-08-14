import { vi, describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { Stomp } from 'stompjs'
import useRealTimeEnvios from '../hooks/useRealTimeEnvios'

const mockConnect = vi.fn()
const mockSubscribe = vi.fn()
const mockDisconnect = vi.fn()
const mockClient = { connect: mockConnect, subscribe: mockSubscribe, disconnect: mockDisconnect }

vi.mock('sockjs-client', () => ({
  __esModule: true,
  default: vi.fn(() => ({}))
}))

vi.mock('stompjs', () => ({
  __esModule: true,
  Stomp: {
    over: vi.fn(() => mockClient)
  }
}))

describe('useRealTimeEnvios', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockConnect.mockImplementation((_headers, onConnect) => {
      onConnect()
    })
  })

  it('se conecta y se suscribe a /topic/envios cuando está habilitado y online', async () => {
    const onMessage = vi.fn()
    renderHook(() => useRealTimeEnvios({ onMessage }))

    expect(Stomp.over).toHaveBeenCalled()
    expect(mockConnect).toHaveBeenCalled()
    expect(mockSubscribe).toHaveBeenCalledWith('/topic/envios', expect.any(Function))
  })

  it('no conecta cuando está deshabilitado', () => {
    renderHook(() => useRealTimeEnvios({ onMessage: vi.fn(), enabled: false }))

    expect(mockConnect).not.toHaveBeenCalled()
    expect(mockSubscribe).not.toHaveBeenCalled()
  })

  it('entrega el mensaje parseado al onMessage', () => {
    const onMessage = vi.fn()
    renderHook(() => useRealTimeEnvios({ onMessage }))

    const frame = {
      body: JSON.stringify({ envioId: 1, tracking: 'MT-1', estado: 'EN_REPARTO' })
    }
    const subscribeCallback = mockSubscribe.mock.calls[0][1]
    act(() => subscribeCallback(frame))

    expect(onMessage).toHaveBeenCalledWith({
      envioId: 1,
      tracking: 'MT-1',
      estado: 'EN_REPARTO'
    })
  })

  it('desconecta al desmontar el componente', () => {
    const { unmount } = renderHook(() => useRealTimeEnvios({ onMessage: vi.fn() }))

    unmount()

    expect(mockDisconnect).toHaveBeenCalled()
  })
})
