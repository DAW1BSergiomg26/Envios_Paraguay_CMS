import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ShipmentDetailPage from './ShipmentDetailPage'

const mockGetAdminEnvioDetalle = vi.fn()
const mockGetDocumentoUrl = vi.fn()
const mockRefreshNow = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('react-router-dom', () => ({
  useParams: () => ({ codigo: 'MT-0001' }),
  useNavigate: () => vi.fn(),
}))

vi.mock('../services/api', () => ({
  getAdminEnvioDetalle: (...args) => mockGetAdminEnvioDetalle(...args),
  getDocumentoUrl: (...args) => mockGetDocumentoUrl(...args),
}))

vi.mock('../hooks/usePolling', () => ({
  default: () => ({ polling: false, lastUpdated: null, refreshNow: mockRefreshNow, refreshError: null }),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

vi.mock('../components/RefreshIndicator', () => ({ default: () => <div /> }))
vi.mock('../components/UpdateEstadoPanel', () => ({ default: () => <div /> }))
vi.mock('../components/Timeline', () => ({ default: () => <div /> }))
vi.mock('../components/EvidenciasGrid', () => ({ default: () => <div /> }))

const ENVIO = {
  codigoUnico: 'MT-0001',
  estado: 'EN_TRANSITO',
  origen: 'Asunción',
  destino: 'Madrid',
  destinatario: 'Ana López',
  peso: '2 kg',
  ultimaActualizacion: '2026-08-10T10:00:00',
  eventos: [],
  evidencias: [],
}

describe('ShipmentDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminEnvioDetalle.mockResolvedValue({ data: ENVIO })
    vi.stubGlobal('open', vi.fn())
  })

  it('abre la etiqueta térmica en pestaña nueva', async () => {
    const user = userEvent.setup()
    mockGetDocumentoUrl.mockReturnValue('/admin/documentos/envios/MT-0001/etiqueta')
    render(<ShipmentDetailPage />)
    const btn = await screen.findByRole('button', { name: /etiqueta térmica/i })
    await user.click(btn)
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('etiqueta', 'MT-0001')
    expect(window.open).toHaveBeenCalledWith('/admin/documentos/envios/MT-0001/etiqueta', '_blank')
  })
})
