import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ImportBatchPage from '../pages/ImportBatchPage'

const mockGetAdminImports = vi.fn()
const mockGetAdminClientes = vi.fn()
const mockUploadImportCsv = vi.fn()
const mockGetAdminImporte = vi.fn()
const mockGetImportErrores = vi.fn()
const mockRefreshNow = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()
const mockGetDocumentoUrl = vi.fn()
const mockDescargarDocumento = vi.fn()

vi.mock('../services/api', () => ({
  getAdminImports: (...args) => mockGetAdminImports(...args),
  getAdminClientes: (...args) => mockGetAdminClientes(...args),
  uploadImportCsv: (...args) => mockUploadImportCsv(...args),
  getAdminImporte: (...args) => mockGetAdminImporte(...args),
  getImportErrores: (...args) => mockGetImportErrores(...args),
  getDocumentoUrl: (...args) => mockGetDocumentoUrl(...args),
  descargarDocumento: (...args) => mockDescargarDocumento(...args),
}))

vi.mock('../hooks/usePolling', () => ({
  default: () => ({ polling: false, lastUpdated: null, refreshNow: mockRefreshNow, refreshError: null }),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const CLIENTES = [
  { id: 7, nombre: 'Cliente Uno' },
  { id: 8, nombre: 'Cliente Dos' },
]

const LOTE = {
  id: 10,
  clienteId: 7,
  nombreArchivo: 'envios.csv',
  totalRegistros: 50,
  procesados: 50,
  exitosos: 48,
  fallidos: 2,
  estado: 'COMPLETADO_CON_ERRORES',
  errorResumen: null,
  fechaCreacion: '2026-08-10T10:00:00',
  fechaFin: '2026-08-10T10:00:05',
}

describe('ImportBatchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminClientes.mockResolvedValue({ data: CLIENTES })
    mockGetAdminImports.mockResolvedValue({ data: [LOTE] })
  })

  it('renderiza título, selector de clientes y botón deshabilitado', async () => {
    render(<ImportBatchPage />)
    expect(await screen.findByText('Carga masiva de envíos (CSV)')).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Sin asignar' })).toBeInTheDocument()
    expect(await screen.findByRole('option', { name: 'Cliente Uno' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /importar csv/i })).toBeDisabled()
  })

  it('sube el CSV y muestra el progreso del lote', async () => {
    const user = userEvent.setup()
    mockUploadImportCsv.mockResolvedValue({ data: { id: 99, estado: 'PROCESANDO' } })
    mockGetAdminImporte.mockResolvedValue({ data: {
      id: 99, estado: 'COMPLETADO', procesados: 10, exitosos: 10, fallidos: 0,
      totalRegistros: 10, nombreArchivo: 'envios.csv',
    } })

    render(<ImportBatchPage />)
    const file = new File(['codigo,estado\nMT-1,RECIBIDO'], 'envios.csv', { type: 'text/csv' })
    await user.upload(screen.getByLabelText('Fichero CSV'), file)
    expect(screen.getByRole('button', { name: /importar csv/i })).toBeEnabled()

    await user.selectOptions(screen.getByRole('combobox'), '7')
    await user.click(screen.getByRole('button', { name: /importar csv/i }))

    expect(mockUploadImportCsv).toHaveBeenCalledTimes(1)
    const [, clienteId] = mockUploadImportCsv.mock.calls[0]
    expect(clienteId).toBe('7')
    expect(await screen.findByText('Lote #99')).toBeInTheDocument()
    expect(await screen.findByText(/COMPLETADO — procesados/)).toBeInTheDocument()
  })

  it('muestra la tabla de lotes recientes con contadores', async () => {
    render(<ImportBatchPage />)
    expect(await screen.findByText('envios.csv')).toBeInTheDocument()
    expect(screen.getByText('COMPLETADO_CON_ERRORES')).toBeInTheDocument()
    expect(screen.getByText('48')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('abre el modal de errores al pulsar Errores', async () => {
    const user = userEvent.setup()
    mockGetImportErrores.mockResolvedValue({ data: [
      { lineaNumero: 3, codigoRastreo: 'MT-3', errorMensaje: 'Estado inválido' },
    ] })

    render(<ImportBatchPage />)
    await user.click(await screen.findByRole('button', { name: /errores/i }))
    expect(mockGetImportErrores).toHaveBeenCalledWith(10)
    expect(await screen.findByText('Estado inválido')).toBeInTheDocument()
    expect(screen.getByText('MT-3')).toBeInTheDocument()
  })

  it('muestra un estado vacío cuando no hay lotes', async () => {
    mockGetAdminImports.mockResolvedValue({ data: [] })
    render(<ImportBatchPage />)
    expect(await screen.findByText('No hay lotes de importación todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga de clientes', async () => {
    mockGetAdminClientes.mockRejectedValue(new Error('Error de conexión'))
    render(<ImportBatchPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })

  it('descarga etiquetas y manifiesto del lote activo', async () => {
    const user = userEvent.setup()
    mockUploadImportCsv.mockResolvedValue({ data: { id: 99, estado: 'PROCESANDO' } })
    mockGetAdminImporte.mockResolvedValue({ data: {
      id: 99, estado: 'COMPLETADO', procesados: 10, exitosos: 10, fallidos: 0,
      totalRegistros: 10, nombreArchivo: 'envios.csv',
    } })

    render(<ImportBatchPage />)
    const file = new File(['codigo,estado\nMT-1,RECIBIDO'], 'envios.csv', { type: 'text/csv' })
    await user.upload(screen.getByLabelText('Fichero CSV'), file)
    await user.click(screen.getByRole('button', { name: /importar csv/i }))
    await screen.findByText('Lote #99')

    mockGetDocumentoUrl
      .mockReturnValueOnce('/api/v1/admin/documentos/lotes/99/etiquetas')
      .mockReturnValueOnce('/api/v1/admin/documentos/lotes/99/manifiesto')

    await user.click(screen.getByRole('button', { name: /etiquetas del lote/i }))
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('etiquetas-lote', 99)
    expect(mockDescargarDocumento).toHaveBeenCalledWith('/api/v1/admin/documentos/lotes/99/etiquetas')

    await user.click(screen.getByRole('button', { name: /manifiesto/i }))
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('manifiesto', 99)
    expect(mockDescargarDocumento).toHaveBeenCalledWith('/api/v1/admin/documentos/lotes/99/manifiesto')
  })
})
