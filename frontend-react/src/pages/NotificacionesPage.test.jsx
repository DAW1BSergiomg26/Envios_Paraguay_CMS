import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import NotificacionesPage from './NotificacionesPage'

const mockListarNotificaciones = vi.fn()
const mockReintentarNotificacion = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  listarNotificaciones: (...args) => mockListarNotificaciones(...args),
  reintentarNotificacion: (...args) => mockReintentarNotificacion(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const NOTIFICACIONES = [
  {
    id: 30,
    envioId: 1,
    destinatario: 'ana@correo.com',
    asunto: 'Tu paquete fue enviado',
    mensaje: 'El envío 1 está en camino.',
    estado: 'ENVIADO',
    errorMensaje: null,
    fechaCreacion: '2026-07-29T12:00:00',
  },
  {
    id: 31,
    envioId: 2,
    destinatario: 'carlos@correo.com',
    asunto: 'Falló el despacho',
    mensaje: 'No se pudo notificar el estado.',
    estado: 'FALLIDO',
    errorMensaje: 'SMTP timeout',
    fechaCreacion: '2026-07-29T13:00:00',
  },
  {
    id: 32,
    envioId: 3,
    destinatario: null,
    asunto: 'Sin destinatario',
    mensaje: 'Envío omitido por falta de correo.',
    estado: 'OMITIDO_SIN_DESTINATARIO',
    errorMensaje: null,
    fechaCreacion: '2026-07-29T14:00:00',
  },
]

describe('NotificacionesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListarNotificaciones.mockResolvedValue({ data: NOTIFICACIONES })
    mockReintentarNotificacion.mockResolvedValue({ data: {} })
  })

  it('cargaYListaNotificaciones', async () => {
    render(<NotificacionesPage />)
    expect(mockListarNotificaciones).toHaveBeenCalled()
    expect(await screen.findByText('Tu paquete fue enviado')).toBeInTheDocument()
    expect(screen.getByText('Falló el despacho')).toBeInTheDocument()
    expect(screen.getByText('Sin destinatario')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('filtraPorEstado', async () => {
    const user = userEvent.setup()
    render(<NotificacionesPage />)
    await screen.findByText('Tu paquete fue enviado')

    await user.selectOptions(screen.getByLabelText('Filtrar por estado'), 'FALLIDO')

    await waitFor(() => expect(mockListarNotificaciones).toHaveBeenLastCalledWith('FALLIDO'))
  })

  it('expandeDetalle', async () => {
    const user = userEvent.setup()
    render(<NotificacionesPage />)
    await screen.findByText('Tu paquete fue enviado')

    await user.click(screen.getByText('Tu paquete fue enviado'))

    expect(await screen.findByText('ana@correo.com')).toBeInTheDocument()
    expect(screen.getByText('El envío 1 está en camino.')).toBeInTheDocument()
  })

  it('reintentaConExito', async () => {
    const user = userEvent.setup()
    render(<NotificacionesPage />)
    await screen.findByText('Falló el despacho')

    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    await waitFor(() => expect(mockReintentarNotificacion).toHaveBeenCalledWith(31))
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(mockListarNotificaciones).toHaveBeenCalledTimes(2)
  })

  it('reintentaConError', async () => {
    mockReintentarNotificacion.mockRejectedValueOnce(new Error('SMTP caído'))
    const user = userEvent.setup()
    render(<NotificacionesPage />)
    await screen.findByText('Falló el despacho')

    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
    expect(mockListarNotificaciones).toHaveBeenCalledTimes(2)
  })
})
