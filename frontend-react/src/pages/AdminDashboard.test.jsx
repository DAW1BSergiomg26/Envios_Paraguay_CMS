import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AdminDashboard from './AdminDashboard'

const mockNavigate = vi.fn()
const mockGetAdminEnvios = vi.fn()
const mockDeleteAdminEnvio = vi.fn()
const mockRefreshNow = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}))

vi.mock('../services/api', () => ({
  getAdminEnvios: (...args) => mockGetAdminEnvios(...args),
  deleteAdminEnvio: (...args) => mockDeleteAdminEnvio(...args),
}))

vi.mock('../hooks/usePolling', () => ({
  default: () => ({ polling: false, lastUpdated: null, refreshNow: mockRefreshNow, refreshError: null }),
}))

vi.mock('../hooks/useRealTimeEnvios', () => ({
  default: () => ({ connected: false }),
}))

vi.mock('../hooks/useOnlineStatus', () => ({
  useOnlineStatus: () => ({ isOnline: true }),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showError: mockShowError, showWarning: vi.fn(), showSuccess: mockShowSuccess, showInfo: vi.fn() }),
}))

vi.mock('../services/offlineCache', () => ({
  saveDashboardCache: vi.fn(),
  getDashboardCache: vi.fn(() => null),
}))

vi.mock('../components/RefreshIndicator', () => ({ default: () => <div /> }))
vi.mock('../components/AppleHero', () => ({ default: () => <div /> }))
vi.mock('../components/AnalyticsSection', () => ({ default: () => <div /> }))
vi.mock('../components/StatsCard', () => ({ default: () => <div /> }))
vi.mock('../components/StatusBadge', () => ({ default: () => <span>estado</span> }))
vi.mock('../components/Pagination', () => ({ default: () => <div /> }))
vi.mock('../components/SearchBar', () => ({ default: () => <div /> }))
vi.mock('../components/MultiStatusFilter', () => ({ default: () => <div /> }))
vi.mock('../components/DateRangeFilter', () => ({ default: () => <div /> }))
vi.mock('../components/ActiveFilters', () => ({ default: () => <div /> }))
vi.mock('../components/ExportButtons', () => ({ default: () => <div /> }))
vi.mock('../components/EmptyState', () => ({ default: () => <div /> }))
vi.mock('../components/OfflineBanner', () => ({ default: () => <div /> }))
vi.mock('../components/SkeletonLoader', () => ({ SkeletonRow: () => <tr />, SkeletonCard: () => <div /> }))

const ENVIOS = {
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

describe('AdminDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminEnvios.mockResolvedValue(ENVIOS)
  })

  it('botonNuevoEnvio_navega al formulario de alta', async () => {
    const user = userEvent.setup()
    render(<AdminDashboard />)
    const btn = await screen.findByRole('button', { name: /nuevo envío/i })
    await user.click(btn)
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envios/nuevo')
  })

  it('accionEditar_navega al formulario de edición', async () => {
    const user = userEvent.setup()
    render(<AdminDashboard />)
    const btn = await screen.findByRole('button', { name: /editar/i })
    await user.click(btn)
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envios/MT-0001/editar')
  })

  it('eliminar_confirmaYRecarga la lista', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockDeleteAdminEnvio.mockResolvedValue({ data: {} })
    render(<AdminDashboard />)
    const btn = await screen.findByRole('button', { name: /eliminar/i })
    await user.click(btn)
    expect(confirmSpy).toHaveBeenCalled()
    expect(mockDeleteAdminEnvio).toHaveBeenCalledWith('MT-0001')
    await waitFor(() => expect(mockGetAdminEnvios.mock.calls.length).toBeGreaterThan(1))
  })
})
