import { render, screen, waitFor, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EnviosAdminView from './EnviosAdminView'

const mockNavigate = vi.fn()
const mockFetchEnvios = vi.fn()
const mockDeleteEnvio = vi.fn()
const mockRefreshNow = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()
const mockShowInfo = vi.fn()

let wsHandler = null
let onDataReport = null

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}))

vi.mock('./trackingService', () => ({
  fetchEnvios: (...args) => mockFetchEnvios(...args),
  deleteEnvio: (...args) => mockDeleteEnvio(...args),
}))

vi.mock('../../hooks/usePolling', () => ({
  default: () => ({ polling: false, lastUpdated: null, refreshNow: mockRefreshNow, refreshError: null }),
}))

vi.mock('../../hooks/useWebSocket', () => ({
  default: (opts) => { wsHandler = opts.onMessage; return { connected: false } },
}))

vi.mock('../../context/NotificationContext', () => ({
  useToast: () => ({ showError: mockShowError, showWarning: vi.fn(), showSuccess: mockShowSuccess, showInfo: mockShowInfo }),
}))

vi.mock('../../services/offlineCache', () => ({
  saveDashboardCache: vi.fn(),
  getDashboardCache: vi.fn(() => null),
}))

vi.mock('../../components/ExportButtons', () => ({ default: () => <div /> }))

const RESPUESTA = {
  data: {
    content: [{
      codigoUnico: 'MT-0001',
      estado: 'EN_TRANSITO',
      destinatario: 'Ana López',
      origen: 'Asunción',
      destino: 'Madrid',
      ultimaActualizacion: '2026-08-10T10:00:00',
    }],
    totalElements: 1,
    totalPages: 1,
  },
}

const FILTROS_BASE = {
  page: 0,
  size: 10,
  estados: [],
  query: '',
  fechaDesde: '',
  fechaHasta: '',
}

function reportar(datos) {
  onDataReport = datos
}

describe('EnviosAdminView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    wsHandler = null
    onDataReport = null
    mockFetchEnvios.mockResolvedValue(RESPUESTA)
  })

  it('fetch inicial con filtros base y reporta datos a onDataChange', async () => {
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(mockFetchEnvios).toHaveBeenCalledWith(FILTROS_BASE))
    await waitFor(() => expect(onDataReport).toBeTruthy())
    expect(onDataReport.total).toBe(1)
    expect(onDataReport.sessionOk).toBe(true)
    expect(onDataReport.loading).toBe(false)
  })

  it('al seleccionar un estado filtra y resetea la página a 0', async () => {
    const user = userEvent.setup()
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(mockFetchEnvios).toHaveBeenCalledTimes(1))
    await user.click(screen.getByRole('button', { name: /en tránsito/i }))
    await waitFor(() =>
      expect(mockFetchEnvios).toHaveBeenCalledWith({ ...FILTROS_BASE, estados: ['EN_TRANSITO'] })
    )
  })

  it('mensaje WS para envío visible dispara refetch silencioso y notifica', async () => {
    render(<EnviosAdminView onDataChange={reportar} />)
    await screen.findByText('MT-0001')
    const callsBefore = mockFetchEnvios.mock.calls.length
    act(() => { wsHandler({ tracking: 'MT-0001', estado: 'ENTREGADO' }) })
    await waitFor(() => expect(mockFetchEnvios.mock.calls.length).toBeGreaterThan(callsBefore))
    expect(mockShowInfo).toHaveBeenCalledWith('Envío MT-0001 actualizado a Entregado')
  })

  it('mensaje WS para envío no visible no refetch', async () => {
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(mockFetchEnvios).toHaveBeenCalledTimes(1))
    wsHandler({ tracking: 'MT-9999', estado: 'ENTREGADO' })
    expect(mockFetchEnvios.mock.calls.length).toBe(1)
    expect(mockShowInfo).not.toHaveBeenCalled()
  })

  it('eliminar confirma, llama deleteEnvio y recarga', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockDeleteEnvio.mockResolvedValue({ data: {} })
    render(<EnviosAdminView onDataChange={reportar} />)
    const btn = await screen.findByRole('button', { name: /eliminar/i })
    await user.click(btn)
    expect(confirmSpy).toHaveBeenCalledWith('¿Eliminar el envío MT-0001?')
    expect(mockDeleteEnvio).toHaveBeenCalledWith('MT-0001')
    await waitFor(() => expect(mockFetchEnvios.mock.calls.length).toBeGreaterThan(1))
  })

  it('refreshNow reportado recarga y muestra toast de éxito', async () => {
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(onDataReport).toBeTruthy())
    await onDataReport.refreshNow()
    expect(mockRefreshNow).toHaveBeenCalled()
    expect(mockShowSuccess).toHaveBeenCalledWith('Datos actualizados')
  })

  it('cae a cache offline en error de red', async () => {
    const cached = { data: { content: [{ codigoUnico: 'MT-CACHE', estado: 'RECIBIDO', destinatario: 'Cache', origen: 'X', destino: 'Y', ultimaActualizacion: '2026-08-01T00:00:00' }], totalElements: 1, totalPages: 1 } }
    const { getDashboardCache } = await import('../../services/offlineCache')
    getDashboardCache.mockReturnValue(cached)
    mockFetchEnvios.mockRejectedValue(new Error('Network Error'))
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(screen.getByText('MT-CACHE')).toBeInTheDocument())
    expect(mockShowInfo).toHaveBeenCalledWith('Mostrando datos offline')
    expect(mockShowError).not.toHaveBeenCalled()
  })

  it('errores de sesión se reportan con sessionOk false', async () => {
    mockFetchEnvios.mockRejectedValue(new Error('No autorizado: la sesión ha expirado'))
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(onDataReport?.sessionOk).toBe(false))
    expect(onDataReport?.error).toBeTruthy()
  })

  it('click en fila navega al detalle y Editar al formulario', async () => {
    const user = userEvent.setup()
    render(<EnviosAdminView onDataChange={reportar} />)
    await screen.findByText('MT-0001')
    await user.click(screen.getByText('Ana López'))
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envio/MT-0001')
    await user.click(screen.getByRole('button', { name: /editar/i }))
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envios/MT-0001/editar')
  })

  it('cambiar tamaño de página refetch con el nuevo size', async () => {
    const user = userEvent.setup()
    render(<EnviosAdminView onDataChange={reportar} />)
    await waitFor(() => expect(mockFetchEnvios).toHaveBeenCalledTimes(1))
    await user.selectOptions(screen.getByLabelText('Resultados por página'), '25')
    await waitFor(() =>
      expect(mockFetchEnvios).toHaveBeenCalledWith({ ...FILTROS_BASE, size: 25 })
    )
  })
})
