import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import DocumentosPage from './DocumentosPage'

const mockGetAdminDocumentos = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminDocumentos: (...args) => mockGetAdminDocumentos(...args),
  formatPesoBytes: (bytes) => `${(bytes / 1024).toFixed(1)} KB`,
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const EMISIONES = [
  {
    id: 1, tipo: 'ETIQUETA_TERMICA', referenciaId: 'MT-0001',
    nombreArchivo: 'etiqueta-MT-0001.pdf', pesoBytes: 20480,
    usuarioGeneracion: 'admin', fechaCreacion: '2026-08-10T09:00:00',
  },
  {
    id: 2, tipo: 'MANIFIESTO_CARGA', referenciaId: '10',
    nombreArchivo: 'manifiesto-lote-10.pdf', pesoBytes: 1536,
    usuarioGeneracion: 'admin', fechaCreacion: '2026-08-10T10:30:00',
  },
]

describe('DocumentosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminDocumentos.mockResolvedValue({ data: EMISIONES })
  })

  it('carga y muestra las emisiones en la tabla', async () => {
    render(<DocumentosPage />)
    expect(mockGetAdminDocumentos).toHaveBeenCalledWith(undefined)
    expect(await screen.findByText('etiqueta-MT-0001.pdf')).toBeInTheDocument()
    expect(screen.getByText('manifiesto-lote-10.pdf')).toBeInTheDocument()
    expect(screen.getByText('20.0 KB')).toBeInTheDocument()
    expect(screen.getByText('ETIQUETA_TERMICA')).toBeInTheDocument()
  })

  it('filtra por tipo al cambiar el select', async () => {
    const user = userEvent.setup()
    render(<DocumentosPage />)
    await screen.findByText('etiqueta-MT-0001.pdf')
    mockGetAdminDocumentos.mockClear()

    await user.selectOptions(screen.getByLabelText('Filtrar por tipo'), 'MANIFIESTO_CARGA')
    expect(mockGetAdminDocumentos).toHaveBeenCalledWith('MANIFIESTO_CARGA')
  })

  it('muestra el estado vacío sin emisiones', async () => {
    mockGetAdminDocumentos.mockResolvedValue({ data: [] })
    render(<DocumentosPage />)
    expect(await screen.findByText('No hay documentos generados todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga', async () => {
    mockGetAdminDocumentos.mockRejectedValue(new Error('Error de conexión'))
    render(<DocumentosPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })
})
