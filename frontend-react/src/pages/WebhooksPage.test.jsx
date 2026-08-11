import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import WebhooksPage from './WebhooksPage'

const mockGetAdminClientes = vi.fn()
const mockListarWebhooks = vi.fn()
const mockCrearWebhook = vi.fn()
const mockActualizarWebhook = vi.fn()
const mockEliminarWebhook = vi.fn()
const mockListarWebhookLogs = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminClientes: (...args) => mockGetAdminClientes(...args),
  listarWebhooks: (...args) => mockListarWebhooks(...args),
  crearWebhook: (...args) => mockCrearWebhook(...args),
  actualizarWebhook: (...args) => mockActualizarWebhook(...args),
  eliminarWebhook: (...args) => mockEliminarWebhook(...args),
  listarWebhookLogs: (...args) => mockListarWebhookLogs(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const CLIENTES = [
  { id: 1, nombre: 'Ana López' },
  { id: 2, nombre: 'Carlos Gómez' },
]

const WEBHOOKS = [
  {
    id: 9,
    clienteId: 1,
    url: 'https://hook.example.com/envios',
    activo: true,
    fechaCreacion: '2026-07-29T12:00:00',
  },
  {
    id: 10,
    clienteId: 2,
    url: 'https://hook2.example.com/envios',
    activo: false,
    fechaCreacion: '2026-07-30T09:00:00',
  },
]

const LOGS = [
  {
    id: 50,
    webhookId: 9,
    envioId: 1,
    responseStatus: 200,
    exitoso: true,
    errorMensaje: null,
    fechaCreacion: '2026-07-29T12:05:00',
  },
  {
    id: 51,
    webhookId: 9,
    envioId: 2,
    responseStatus: 500,
    exitoso: false,
    errorMensaje: 'timeout',
    fechaCreacion: '2026-07-29T12:06:00',
  },
]

describe('WebhooksPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminClientes.mockResolvedValue({ data: CLIENTES })
    mockListarWebhooks.mockResolvedValue({ data: WEBHOOKS })
    mockCrearWebhook.mockResolvedValue({ data: {} })
    mockActualizarWebhook.mockResolvedValue({ data: {} })
    mockEliminarWebhook.mockResolvedValue({ data: {} })
    mockListarWebhookLogs.mockResolvedValue({ data: LOGS })
  })

  it('muestraCargaYDatosLista', async () => {
    render(<WebhooksPage />)
    expect(mockListarWebhooks).toHaveBeenCalled()
    expect(await screen.findByText('https://hook.example.com/envios')).toBeInTheDocument()
    expect(screen.getByText('https://hook2.example.com/envios')).toBeInTheDocument()
    expect(screen.getByText('Activo')).toBeInTheDocument()
    expect(screen.getByText('Inactivo')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Ver logs' })).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: 'Editar' })).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: 'Eliminar' })).toHaveLength(2)
  })

  it('filtraCliente', async () => {
    const user = userEvent.setup()
    render(<WebhooksPage />)
    await screen.findByText('https://hook.example.com/envios')

    const filtro = screen.getByLabelText('Filtrar por cliente')
    await user.selectOptions(filtro, '1')

    await waitFor(() => expect(mockListarWebhooks).toHaveBeenLastCalledWith(1))
  })

  it('creaWebhook', async () => {
    const user = userEvent.setup()
    render(<WebhooksPage />)
    await screen.findByText('https://hook.example.com/envios')

    await user.click(screen.getByRole('button', { name: /Nuevo webhook/i }))
    await user.selectOptions(screen.getByLabelText('Cliente'), '1')
    await user.type(screen.getByLabelText('URL del webhook'), 'https://nuevo.example.com/ws')
    await user.type(screen.getByLabelText('Secret token'), 'tok123')

    await user.click(screen.getByRole('button', { name: 'Crear webhook' }))

    await waitFor(() =>
      expect(mockCrearWebhook).toHaveBeenCalledWith({
        clienteId: 1,
        url: 'https://nuevo.example.com/ws',
        secretToken: 'tok123',
        activo: true,
      })
    )
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(mockListarWebhooks).toHaveBeenCalledTimes(2)
  })

  it('editaWebhook', async () => {
    const user = userEvent.setup()
    render(<WebhooksPage />)
    await screen.findByText('https://hook.example.com/envios')

    await user.click(screen.getAllByRole('button', { name: 'Editar' })[0])

    const urlInput = screen.getByLabelText('URL del webhook')
    await user.clear(urlInput)
    await user.type(urlInput, 'https://editado.example.com/ws')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() =>
      expect(mockActualizarWebhook).toHaveBeenCalledWith(9, {
        url: 'https://editado.example.com/ws',
        activo: true,
      })
    )
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('eliminaWebhookConConfirmacion', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<WebhooksPage />)
    await screen.findByText('https://hook.example.com/envios')

    await user.click(screen.getAllByRole('button', { name: 'Eliminar' })[0])

    await waitFor(() => expect(mockEliminarWebhook).toHaveBeenCalledWith(9))
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(mockListarWebhooks).toHaveBeenCalledTimes(2)
    confirmSpy.mockRestore()
  })

  it('cancelaEliminacion', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<WebhooksPage />)
    await screen.findByText('https://hook.example.com/envios')

    await user.click(screen.getAllByRole('button', { name: 'Eliminar' })[0])

    expect(mockEliminarWebhook).not.toHaveBeenCalled()
    expect(mockListarWebhooks).toHaveBeenCalledTimes(1)
    confirmSpy.mockRestore()
  })

  it('logsDesplegables', async () => {
    const user = userEvent.setup()
    render(<WebhooksPage />)
    await screen.findByText('https://hook.example.com/envios')

    await user.click(screen.getAllByRole('button', { name: 'Ver logs' })[0])

    await waitFor(() => expect(mockListarWebhookLogs).toHaveBeenCalledWith(9))
    expect(await screen.findByText('timeout')).toBeInTheDocument()
    expect(screen.getByText('200')).toBeInTheDocument()
    expect(screen.getByText('500')).toBeInTheDocument()
  })

  it('muestraErrorSiFallaCarga', async () => {
    mockListarWebhooks.mockRejectedValueOnce(new Error('Error de conexión'))
    const user = userEvent.setup()
    render(<WebhooksPage />)

    expect(await screen.findByText(/Error de conexión/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Recargar datos/i }))
    await waitFor(() => expect(mockListarWebhooks).toHaveBeenCalledTimes(2))
  })
})
