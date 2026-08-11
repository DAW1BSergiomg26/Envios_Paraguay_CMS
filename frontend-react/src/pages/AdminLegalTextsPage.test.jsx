import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AdminLegalTextsPage from './AdminLegalTextsPage'

const mockGetAdminTextos = vi.fn()
const mockGetTextoLegal = vi.fn()
const mockPutTextoLegal = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminTextos: (...args) => mockGetAdminTextos(...args),
  getTextoLegal: (...args) => mockGetTextoLegal(...args),
  putTextoLegal: (...args) => mockPutTextoLegal(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const TEXTOS_LISTA = [
  { id: 1, slug: 'aviso-legal', titulo: 'Aviso Legal', updatedAt: '2026-05-12T12:00:00' },
  { id: 2, slug: 'politica-cookies', titulo: 'Política de Cookies', updatedAt: '2026-05-10T09:30:00' },
]

const AVISO_LEGAL_DETALLE = {
  id: 1,
  slug: 'aviso-legal',
  titulo: 'Aviso Legal',
  contenido: 'Contenido extenso del aviso legal...',
  updatedAt: '2026-05-12T12:00:00',
}

describe('AdminLegalTextsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminTextos.mockResolvedValue({ data: TEXTOS_LISTA })
    mockGetTextoLegal.mockResolvedValue({ data: AVISO_LEGAL_DETALLE })
    mockPutTextoLegal.mockResolvedValue({ data: AVISO_LEGAL_DETALLE })
  })

  it('carga y muestra la lista de textos legales en el panel izquierdo', async () => {
    render(<AdminLegalTextsPage />)
    expect(mockGetAdminTextos).toHaveBeenCalled()
    expect(await screen.findByText('Aviso Legal')).toBeInTheDocument()
    expect(screen.getByText('Política de Cookies')).toBeInTheDocument()
  })

  it('abre el detalle al hacer click en un texto de la lista', async () => {
    const user = userEvent.setup()
    render(<AdminLegalTextsPage />)
    await screen.findByText('Aviso Legal')

    await user.click(screen.getByText('Aviso Legal'))

    expect(mockGetTextoLegal).toHaveBeenCalledWith('aviso-legal')
    expect(await screen.findByDisplayValue('Contenido extenso del aviso legal...')).toBeInTheDocument()
  })

  it('guarda los cambios editados enviando PUT', async () => {
    const user = userEvent.setup()
    render(<AdminLegalTextsPage />)
    await screen.findByText('Aviso Legal')
    await user.click(screen.getByText('Aviso Legal'))
    await screen.findByDisplayValue('Contenido extenso del aviso legal...')

    const textarea = screen.getByLabelText(/Contenido/i)
    await user.clear(textarea)
    await user.type(textarea, 'Nuevo contenido legal modificado.')

    const guardarBtn = screen.getByRole('button', { name: /Guardar cambios/i })
    await user.click(guardarBtn)

    await waitFor(() => expect(mockPutTextoLegal).toHaveBeenCalledWith('aviso-legal', {
      titulo: 'Aviso Legal',
      contenido: 'Nuevo contenido legal modificado.',
    }))
    expect(mockShowSuccess).toHaveBeenCalled()
  })

  it('recarga la lista tras guardar exitosamente', async () => {
    const user = userEvent.setup()
    render(<AdminLegalTextsPage />)
    await screen.findByText('Aviso Legal')
    await user.click(screen.getByText('Aviso Legal'))
    await screen.findByDisplayValue('Contenido extenso del aviso legal...')

    const textarea = screen.getByLabelText(/Contenido/i)
    await user.type(textarea, ' Mas texto')

    const guardarBtn = screen.getByRole('button', { name: /Guardar cambios/i })
    await user.click(guardarBtn)

    await waitFor(() => expect(mockGetAdminTextos).toHaveBeenCalledTimes(2))
  })

  it('muestra toast de error si falla la carga del servicio', async () => {
    mockGetAdminTextos.mockRejectedValue(new Error('Error de servidor'))
    render(<AdminLegalTextsPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })

  it('muestra mensaje cuando no hay texto seleccionado', async () => {
    mockGetAdminTextos.mockResolvedValue({ data: [] })
    render(<AdminLegalTextsPage />)
    expect(await screen.findByText('Selecciona un texto legal para editar')).toBeInTheDocument()
  })
})
