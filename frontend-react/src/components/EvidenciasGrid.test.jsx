import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EvidenciasGrid from './EvidenciasGrid'

const mockDescargarDocumento = vi.fn()
vi.mock('../services/api', () => ({
  descargarDocumento: (...args) => mockDescargarDocumento(...args),
}))

const EVIDENCIAS = [
  { titulo: 'Guía de embarque', descripcion: 'Firmada por el capitán', tipo: 'DOCUMENTO', urlArchivo: '/uploads/evidencias/guia.pdf' },
  { titulo: 'Foto del envío', descripcion: 'Estado de la mercancía', tipo: 'FOTO', urlArchivo: '/uploads/evidencias/foto.jpg' },
]

describe('EvidenciasGrid', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra el estado vacío sin evidencias', () => {
    render(<EvidenciasGrid evidencias={[]} />)
    expect(screen.getByText('No hay evidencias registradas para este envío')).toBeInTheDocument()
  })

  it('renderiza las tarjetas con título y descripción', () => {
    render(<EvidenciasGrid evidencias={EVIDENCIAS} />)
    expect(screen.getByText('Guía de embarque')).toBeInTheDocument()
    expect(screen.getByText('Foto del envío')).toBeInTheDocument()
  })

  it('descarga la evidencia al pulsar el botón de descarga', async () => {
    const user = userEvent.setup()
    render(<EvidenciasGrid evidencias={EVIDENCIAS} />)
    const botones = screen.getAllByRole('button', { name: /descargar/i })
    await user.click(botones[0])
    expect(mockDescargarDocumento).toHaveBeenCalledWith('/uploads/evidencias/guia.pdf')
  })

  it('abre el modal de preview al hacer clic en la imagen', async () => {
    const user = userEvent.setup()
    render(<EvidenciasGrid evidencias={EVIDENCIAS} />)
    await user.click(screen.getByAltText('Guía de embarque'))
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '✕' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
