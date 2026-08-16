import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import AdminDashboard from './AdminDashboard'

const mockNavigate = vi.fn()
const mockRefreshNow = vi.fn()

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}))

vi.mock('../hooks/useOnlineStatus', () => ({
  useOnlineStatus: () => ({ isOnline: true }),
}))

vi.mock('../components/RefreshIndicator', () => ({
  default: ({ onRefresh }) => <button type="button" onClick={onRefresh}>Actualizar</button>,
}))

vi.mock('../components/AppleHero', () => ({ default: () => <div /> }))
vi.mock('../components/OfflineBanner', () => ({ default: () => <div /> }))
vi.mock('../components/AnalyticsSection', () => ({ default: () => <div /> }))
vi.mock('../components/StatsCard', () => ({ default: ({ label, value }) => <div data-testid="stat">{label}: {value}</div> }))
vi.mock('../components/SkeletonLoader', () => ({ SkeletonRow: () => <tr />, SkeletonCard: () => <div /> }))

let report

vi.mock('../features/tracking/EnviosAdminView', () => ({
  default: ({ onDataChange }) => {
    useEffect(() => { onDataChange(report) }, [onDataChange])
    return <div data-testid="envios-admin-view" />
  },
}))

const baseReport = {
  envios: [{
    codigoUnico: 'MT-0001',
    estado: 'EN_TRANSITO',
    destinatario: 'Ana López',
    origen: 'Asunción',
    destino: 'Madrid',
    ultimaActualizacion: '2026-08-10T10:00:00',
  }],
  total: 1,
  totalPages: 1,
  loading: false,
  error: null,
  sessionOk: true,
  lastUpdated: null,
  polling: false,
  refreshError: null,
  refreshNow: mockRefreshNow,
}

describe('AdminDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    report = { ...baseReport }
  })

  it('botonNuevoEnvio_navega al formulario de alta', async () => {
    const user = userEvent.setup()
    render(<AdminDashboard />)
    const btn = await screen.findByRole('button', { name: /nuevo envío/i })
    await user.click(btn)
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envios/nuevo')
  })

  it('renderiza EnviosAdminView como fuente de datos', async () => {
    render(<AdminDashboard />)
    expect(await screen.findByTestId('envios-admin-view')).toBeInTheDocument()
  })

  it('muestra las estadísticas derivadas del reporte', async () => {
    render(<AdminDashboard />)
    expect(await screen.findByText(/Total envíos: 1/)).toBeInTheDocument()
    expect(screen.getByText(/En tránsito: 1/)).toBeInTheDocument()
    expect(screen.getByText(/Entregados: 0/)).toBeInTheDocument()
    expect(screen.getByText(/En aduana: 0/)).toBeInTheDocument()
    expect(screen.getByText(/Pendientes: 0/)).toBeInTheDocument()
  })

  it('botonActualizar_dispara refreshNow del reporte', async () => {
    const user = userEvent.setup()
    render(<AdminDashboard />)
    await screen.findByText(/Total envíos: 1/)
    const btn = await screen.findByRole('button', { name: /actualizar/i })
    await user.click(btn)
    expect(mockRefreshNow).toHaveBeenCalled()
  })

  it('errorGeneral_muestraBanner', async () => {
    report = { ...baseReport, error: 'Error de red' }
    render(<AdminDashboard />)
    expect(await screen.findByText('Error de red')).toBeInTheDocument()
  })

  it('errorSesion_muestraBannerDeAutenticacion', async () => {
    report = { ...baseReport, error: 'Sesión expirada', sessionOk: false }
    render(<AdminDashboard />)
    expect(await screen.findByText('Se requiere autenticación')).toBeInTheDocument()
  })
})
