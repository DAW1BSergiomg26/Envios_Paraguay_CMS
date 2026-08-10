import { render, screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ReservasPage from './ReservasPage'

const mockGetAdminReservas = vi.fn()
const mockPatchAdminReservaEstado = vi.fn()
const mockPutAdminReserva = vi.fn()
const mockDeleteAdminReserva = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminReservas: (...args) => mockGetAdminReservas(...args),
  patchAdminReservaEstado: (...args) => mockPatchAdminReservaEstado(...args),
  putAdminReserva: (...args) => mockPutAdminReserva(...args),
  deleteAdminReserva: (...args) => mockDeleteAdminReserva(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const RESERVAS = [
  {
    id: 1, nombreCliente: 'Juan Pérez', email: 'juan@example.com', telefono: '+34 611 111 111',
    fechaEntrada: '2026-06-01', fechaSalida: '2026-06-05', numeroHuespedes: 4,
    comentarios: 'Solicito envío de 4 palets', estado: 'pendiente', createdAt: '2026-05-20T10:30:00',
  },
  {
    id: 2, nombreCliente: 'Laura Martínez', email: 'laura@example.com', telefono: '+34 622 222 222',
    fechaEntrada: '2026-06-10', fechaSalida: '2026-06-15', numeroHuespedes: 2,
    comentarios: 'Documentación urgente', estado: 'confirmada', createdAt: '2026-05-18T15:45:00',
  },
]

describe('ReservasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminReservas.mockResolvedValue({ data: RESERVAS })
    mockPatchAdminReservaEstado.mockResolvedValue({ data: {} })
    mockPutAdminReserva.mockResolvedValue({ data: {} })
    mockDeleteAdminReserva.mockResolvedValue({ data: {} })
  })

  it('carga y muestra las reservas en la tabla', async () => {
    render(<ReservasPage />)
    expect(mockGetAdminReservas).toHaveBeenCalledWith(undefined)
    expect(await screen.findByText('Juan Pérez')).toBeInTheDocument()
    expect(screen.getByText('Laura Martínez')).toBeInTheDocument()
    const table = screen.getByRole('table')
    expect(await within(table).findByText('Pendiente')).toBeInTheDocument()
    expect(within(table).getByText('Confirmada')).toBeInTheDocument()
  })

  it('filtra por estado al cambiar el select', async () => {
    const user = userEvent.setup()
    render(<ReservasPage />)
    await screen.findByText('Juan Pérez')
    mockGetAdminReservas.mockClear()

    await user.selectOptions(screen.getByLabelText('Filtrar por estado'), 'aprobada')
    expect(mockGetAdminReservas).toHaveBeenCalledWith('aprobada')
  })

  it('aprueba una reserva pendiente', async () => {
    const user = userEvent.setup()
    render(<ReservasPage />)
    const aprobar = await screen.findByRole('button', { name: /Aprobar/i })
    await user.click(aprobar)

    await waitFor(() => expect(mockPatchAdminReservaEstado).toHaveBeenCalledWith(1, 'aprobada'))
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('cancela una reserva con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<ReservasPage />)
    const cancelar = await screen.findByRole('button', { name: /Cancelar/i })
    await user.click(cancelar)

    await waitFor(() => expect(mockPatchAdminReservaEstado).toHaveBeenCalledWith(2, 'cancelada'))
    confirmSpy.mockRestore()
  })

  it('abre el modal de edición, guarda con PUT y muestra toast', async () => {
    const user = userEvent.setup()
    render(<ReservasPage />)
    const editar = await screen.findAllByRole('button', { name: /Editar/i })
    await user.click(editar[0])

    const guardar = await screen.findByRole('button', { name: /Guardar/i })
    await user.click(guardar)

    await waitFor(() => expect(mockPutAdminReserva).toHaveBeenCalled())
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('elimina una reserva con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<ReservasPage />)
    const eliminar = await screen.findAllByRole('button', { name: /Eliminar/i })
    await user.click(eliminar[0])

    await waitFor(() => expect(mockDeleteAdminReserva).toHaveBeenCalledWith(1))
    confirmSpy.mockRestore()
  })

  it('muestra el estado vacío sin reservas', async () => {
    mockGetAdminReservas.mockResolvedValue({ data: [] })
    render(<ReservasPage />)
    expect(await screen.findByText('No hay reservas todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga', async () => {
    mockGetAdminReservas.mockRejectedValue(new Error('Error de conexión'))
    render(<ReservasPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })
})
