import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MensajesPage from './MensajesPage'

const mockGetAdminMensajes = vi.fn()
const mockPatchAdminMensajeLeido = vi.fn()
const mockDeleteAdminMensaje = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminMensajes: (...args) => mockGetAdminMensajes(...args),
  patchAdminMensajeLeido: (...args) => mockPatchAdminMensajeLeido(...args),
  deleteAdminMensaje: (...args) => mockDeleteAdminMensaje(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const MENSAJES = [
  {
    id: 1, nombre: 'Ana López', email: 'ana@example.com', telefono: '+34 644 444 444',
    mensaje: 'Hola, quiero información sobre envíos a Asunción', leido: false,
    fechaEnvio: '2026-05-12T12:00:00',
  },
  {
    id: 2, nombre: 'Carlos Ruiz', email: 'carlos@example.com', telefono: '+34 655 555 555',
    mensaje: '¿Cuál es el plazo de entrega a Paraguay?', leido: true,
    fechaEnvio: '2026-05-10T09:30:00',
  },
]

describe('MensajesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminMensajes.mockResolvedValue({ data: MENSAJES })
    mockPatchAdminMensajeLeido.mockResolvedValue({ data: {} })
    mockDeleteAdminMensaje.mockResolvedValue({ data: {} })
  })

  it('carga y muestra los mensajes en la tabla', async () => {
    render(<MensajesPage />)
    expect(mockGetAdminMensajes).toHaveBeenCalledWith(undefined)
    expect(await screen.findByText('Ana López')).toBeInTheDocument()
    expect(screen.getByText('Carlos Ruiz')).toBeInTheDocument()
    expect(screen.getByText('Leído')).toBeInTheDocument()
    expect(screen.getByText('No leído')).toBeInTheDocument()
  })

  it('filtra por estado de lectura al cambiar el select', async () => {
    const user = userEvent.setup()
    render(<MensajesPage />)
    await screen.findByText('Ana López')
    mockGetAdminMensajes.mockClear()

    await user.selectOptions(screen.getByLabelText('Filtrar por estado de lectura'), 'no_leido')
    expect(mockGetAdminMensajes).toHaveBeenCalledWith(false)
  })

  it('marca un mensaje como leído', async () => {
    const user = userEvent.setup()
    render(<MensajesPage />)
    const marcar = await screen.findByRole('button', { name: /Marcar leído/i })
    await user.click(marcar)

    await waitFor(() => expect(mockPatchAdminMensajeLeido).toHaveBeenCalledWith(1, true))
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('elimina un mensaje con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<MensajesPage />)
    const eliminar = await screen.findAllByRole('button', { name: /Eliminar/i })
    await user.click(eliminar[0])

    await waitFor(() => expect(mockDeleteAdminMensaje).toHaveBeenCalledWith(1))
    confirmSpy.mockRestore()
  })

  it('muestra el estado vacío sin mensajes', async () => {
    mockGetAdminMensajes.mockResolvedValue({ data: [] })
    render(<MensajesPage />)
    expect(await screen.findByText('No hay mensajes de contacto todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga', async () => {
    mockGetAdminMensajes.mockRejectedValue(new Error('Error de conexión'))
    render(<MensajesPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })
})
