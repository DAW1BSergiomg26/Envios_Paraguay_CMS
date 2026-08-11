import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ShipmentDetailPage from './ShipmentDetailPage'

const mockNavigate = vi.fn()
const mockGetAdminEnvioDetalle = vi.fn()
const mockGetDocumentoUrl = vi.fn()
const mockDeleteAdminEnvio = vi.fn()
const mockUploadAdminEvidencia = vi.fn()
const mockPatchAdminEvidenciaVisibilidad = vi.fn()
const mockRefreshNow = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('react-router-dom', () => ({
  useParams: () => ({ codigo: 'MT-0001' }),
  useNavigate: () => mockNavigate,
}))

vi.mock('../services/api', () => ({
  getAdminEnvioDetalle: (...args) => mockGetAdminEnvioDetalle(...args),
  getDocumentoUrl: (...args) => mockGetDocumentoUrl(...args),
  deleteAdminEnvio: (...args) => mockDeleteAdminEnvio(...args),
  uploadAdminEvidencia: (...args) => mockUploadAdminEvidencia(...args),
  patchAdminEvidenciaVisibilidad: (...args) => mockPatchAdminEvidenciaVisibilidad(...args),
  descargarDocumento: vi.fn(),
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

const ENVIO = {
  codigoUnico: 'MT-0001',
  estado: 'EN_TRANSITO',
  origen: 'Asunción',
  destino: 'Madrid',
  destinatario: 'Ana López',
  peso: '2 kg',
  ultimaActualizacion: '2026-08-10T10:00:00',
  eventos: [],
  evidencias: [{
    id: 1,
    titulo: 'Guía',
    descripcion: '',
    tipo: 'DOCUMENTO',
    urlArchivo: '/uploads/evidencias/guia.pdf',
    visibleCliente: true,
  }],
}

describe('ShipmentDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminEnvioDetalle.mockResolvedValue({ data: ENVIO })
    vi.stubGlobal('open', vi.fn())
  })

  it('abre la etiqueta térmica en pestaña nueva', async () => {
    const user = userEvent.setup()
    mockGetDocumentoUrl.mockReturnValue('/api/v1/admin/documentos/envios/MT-0001/etiqueta')
    render(<ShipmentDetailPage />)
    const btn = await screen.findByRole('button', { name: /etiqueta térmica/i })
    await user.click(btn)
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('etiqueta', 'MT-0001')
    expect(window.open).toHaveBeenCalledWith('/api/v1/admin/documentos/envios/MT-0001/etiqueta', '_blank')
  })

  it('editar_navegaAFormulario', async () => {
    const user = userEvent.setup()
    render(<ShipmentDetailPage />)
    const btn = await screen.findByRole('button', { name: /editar envío/i })
    await user.click(btn)
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard/envios/MT-0001/editar')
  })

  it('eliminar_confirmaYElimina el envío', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockDeleteAdminEnvio.mockResolvedValue({ data: {} })
    render(<ShipmentDetailPage />)
    const btn = await screen.findByRole('button', { name: /eliminar envío/i })
    await user.click(btn)
    expect(confirmSpy).toHaveBeenCalled()
    expect(mockDeleteAdminEnvio).toHaveBeenCalledWith('MT-0001')
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'))
  })

  it('panelEvidenciasAdmin_subirYAlternar', async () => {
    const user = userEvent.setup()
    render(<ShipmentDetailPage />)
    const file = new File(['contenido'], 'evidencia.pdf', { type: 'application/pdf' })
    const input = await screen.findByLabelText('Archivo de evidencia')
    await user.upload(input, file)
    await user.click(screen.getByRole('button', { name: /subir evidencia/i }))
    expect(mockUploadAdminEvidencia).toHaveBeenCalled()
    expect(mockUploadAdminEvidencia.mock.calls[0][0]).toBe('MT-0001')
    await user.click(screen.getByRole('button', { name: /visibilidad/i }))
    expect(mockPatchAdminEvidenciaVisibilidad).toHaveBeenCalledWith(1, false)
  })
})
