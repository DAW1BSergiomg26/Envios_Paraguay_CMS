import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EnvioFormPage from './EnvioFormPage'

const mockPostAdminEnvio = vi.fn()
const mockPutAdminEnvio = vi.fn()
const mockGetAdminEnvioDetalle = vi.fn()
const mockGetAdminClientes = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()
const mockNavigate = vi.fn()

let mockParams = {}

vi.mock('react-router-dom', () => ({
  useParams: () => mockParams,
  useNavigate: () => mockNavigate,
}))

vi.mock('../services/api', () => ({
  postAdminEnvio: (...args) => mockPostAdminEnvio(...args),
  putAdminEnvio: (...args) => mockPutAdminEnvio(...args),
  getAdminEnvioDetalle: (...args) => mockGetAdminEnvioDetalle(...args),
  getAdminClientes: (...args) => mockGetAdminClientes(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

vi.mock('../components/EmptyState', () => ({ default: () => <div /> }))

const CLIENTES = [
  { id: 1, nombre: 'Ana López' },
  { id: 2, nombre: 'Carlos Gómez' },
]

const ENVIO = {
  codigoUnico: 'MT-0001',
  estado: 'EN_TRANSITO',
  destinatario: 'Ana López',
  origen: 'Asunción',
  destino: 'Madrid',
  peso: '2 kg',
  contenido: 'Documentos',
}

describe('EnvioFormPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockNavigate.mockReset()
    mockParams = {}
    mockGetAdminClientes.mockResolvedValue({ data: CLIENTES })
    mockGetAdminEnvioDetalle.mockResolvedValue({ data: ENVIO })
    mockPostAdminEnvio.mockResolvedValue({ data: { codigoUnico: 'MT-2026-0042' } })
    mockPutAdminEnvio.mockResolvedValue({ data: ENVIO })
  })

  it('modo crear muestra el formulario con título y campos', async () => {
    render(<EnvioFormPage />)
    expect(await screen.findByRole('heading', { name: /nuevo envío/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/destinatario/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/estado/i)).toBeInTheDocument()
  })

  it('modo editar carga el detalle y rellena los campos', async () => {
    mockParams = { codigo: 'MT-0001' }
    render(<EnvioFormPage />)
    expect(mockGetAdminEnvioDetalle).toHaveBeenCalledWith('MT-0001')
    expect(await screen.findByLabelText(/destinatario/i)).toHaveValue('Ana López')
    expect(screen.getByLabelText(/estado/i)).toHaveValue('EN_TRANSITO')
    expect(screen.getByLabelText(/código/i)).toHaveValue('MT-0001')
    expect(screen.getByLabelText(/código/i)).toBeDisabled()
  })

  it('crea un envío y navega al detalle', async () => {
    const user = userEvent.setup()
    render(<EnvioFormPage />)
    await screen.findByRole('heading', { name: /nuevo envío/i })

    await user.type(screen.getByLabelText(/destinatario/i), 'María González')
    await user.selectOptions(screen.getByLabelText(/estado/i), 'RECIBIDO')
    await user.type(screen.getByLabelText(/origen/i), 'Asunción')
    await user.type(screen.getByLabelText(/destino/i), 'Madrid')
    await user.click(screen.getByRole('button', { name: /guardar/i }))

    await waitFor(() => expect(mockPostAdminEnvio).toHaveBeenCalled())
    const body = mockPostAdminEnvio.mock.calls[0][0]
    expect(body).toMatchObject({
      destinatario: 'María González',
      estado: 'RECIBIDO',
      origen: 'Asunción',
      destino: 'Madrid',
    })
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envio/MT-2026-0042')
  })

  it('edita un envío y navega al detalle', async () => {
    mockParams = { codigo: 'MT-0001' }
    const user = userEvent.setup()
    render(<EnvioFormPage />)
    await screen.findByLabelText(/destinatario/i)

    await user.clear(screen.getByLabelText(/destinatario/i))
    await user.type(screen.getByLabelText(/destinatario/i), 'Ana López Galán')
    await user.click(screen.getByRole('button', { name: /guardar/i }))

    await waitFor(() => expect(mockPutAdminEnvio).toHaveBeenCalled())
    expect(mockPutAdminEnvio.mock.calls[0][0]).toBe('MT-0001')
    expect(mockPutAdminEnvio.mock.calls[0][1]).toMatchObject({ destinatario: 'Ana López Galán' })
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envio/MT-0001')
  })

  it('valida destinatario obligatorio sin llamar a la API', async () => {
    const user = userEvent.setup()
    render(<EnvioFormPage />)
    await screen.findByRole('heading', { name: /nuevo envío/i })

    await user.click(screen.getByRole('button', { name: /guardar/i }))

    expect(mockPostAdminEnvio).not.toHaveBeenCalled()
    expect(await screen.findByText(/destinatario es obligatorio/i)).toBeInTheDocument()
  })

  it('carga los clientes para el selector', async () => {
    render(<EnvioFormPage />)
    expect(mockGetAdminClientes).toHaveBeenCalled()
    expect(await screen.findByRole('option', { name: 'Ana López' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Carlos Gómez' })).toBeInTheDocument()
  })
})
