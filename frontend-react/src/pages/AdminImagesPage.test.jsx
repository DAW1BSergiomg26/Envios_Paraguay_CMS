import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AdminImagesPage from './AdminImagesPage'

const mockGetAdminImagenes = vi.fn()
const mockUploadAdminImagen = vi.fn()
const mockPatchAdminImagenOrden = vi.fn()
const mockDeleteAdminImagen = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminImagenes: (...args) => mockGetAdminImagenes(...args),
  uploadAdminImagen: (...args) => mockUploadAdminImagen(...args),
  patchAdminImagenOrden: (...args) => mockPatchAdminImagenOrden(...args),
  deleteAdminImagen: (...args) => mockDeleteAdminImagen(...args),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const IMAGENES = [
  {
    id: 1,
    titulo: 'Banner Principal',
    descripcion: 'Imagen del banner de inicio',
    url: '/uploads/banner.jpg',
    categoria: 'banner',
    orden: 1,
    createdAt: '2026-05-12T12:00:00',
  },
  {
    id: 2,
    titulo: 'Logo Marca',
    descripcion: 'Logo corporativo',
    url: '/uploads/logo.png',
    categoria: 'logos',
    orden: 2,
    createdAt: '2026-05-10T09:30:00',
  },
]

describe('AdminImagesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminImagenes.mockResolvedValue({ data: IMAGENES })
    mockUploadAdminImagen.mockResolvedValue({ data: {} })
    mockPatchAdminImagenOrden.mockResolvedValue({ data: {} })
    mockDeleteAdminImagen.mockResolvedValue({ data: {} })
  })

  it('carga y muestra la galería de imágenes', async () => {
    render(<AdminImagesPage />)
    expect(mockGetAdminImagenes).toHaveBeenCalled()
    expect(await screen.findByAltText('Banner Principal')).toBeInTheDocument()
    expect(screen.getByAltText('Logo Marca')).toBeInTheDocument()
    expect(screen.getByText('banner')).toBeInTheDocument()
  })

  it('muestra EmptyState cuando no hay imágenes', async () => {
    mockGetAdminImagenes.mockResolvedValue({ data: [] })
    render(<AdminImagesPage />)
    expect(await screen.findByText('No hay imágenes en la galería')).toBeInTheDocument()
  })

  it('sube una imagen y recarga la galería', async () => {
    const user = userEvent.setup()
    render(<AdminImagesPage />)
    await screen.findByAltText('Banner Principal')

    const file = new File(['fake-img'], 'nueva.jpg', { type: 'image/jpeg' })
    const fileInput = screen.getByLabelText(/Archivo de imagen/i)
    const tituloInput = screen.getByLabelText(/Título/i)

    await user.type(tituloInput, 'Nueva Foto')
    await user.upload(fileInput, file)

    const submitBtn = screen.getByRole('button', { name: /Subir imagen/i })
    await user.click(submitBtn)

    await waitFor(() => expect(mockUploadAdminImagen).toHaveBeenCalled())
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(mockGetAdminImagenes).toHaveBeenCalledTimes(2)
  })

  it('muestra toast de error si la subida falla', async () => {
    const user = userEvent.setup()
    mockUploadAdminImagen.mockRejectedValue(new Error('Archivo demasiado grande'))
    render(<AdminImagesPage />)
    await screen.findByAltText('Banner Principal')

    const file = new File(['fake-img'], 'nueva.jpg', { type: 'image/jpeg' })
    const fileInput = screen.getByLabelText(/Archivo de imagen/i)
    const tituloInput = screen.getByLabelText(/Título/i)

    await user.type(tituloInput, 'Nueva Foto')
    await user.upload(fileInput, file)

    const submitBtn = screen.getByRole('button', { name: /Subir imagen/i })
    await user.click(submitBtn)

    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })

  it('cambia el orden de una imagen y lo persiste', async () => {
    render(<AdminImagesPage />)
    await screen.findByAltText('Banner Principal')

    const ordenInputs = screen.getAllByRole('spinbutton', { name: /Orden de la imagen/i })
    fireEvent.change(ordenInputs[0], { target: { value: '5' } })
    fireEvent.blur(ordenInputs[0])

    await waitFor(() => expect(mockPatchAdminImagenOrden).toHaveBeenCalledWith(1, 5))
  })

  it('elimina una imagen con confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<AdminImagesPage />)
    const eliminarBtns = await screen.findAllByRole('button', { name: /Eliminar/i })

    await user.click(eliminarBtns[0])

    await waitFor(() => expect(mockDeleteAdminImagen).toHaveBeenCalledWith(1))
    expect(mockShowSuccess).toHaveBeenCalled()
    confirmSpy.mockRestore()
  })

  it('no elimina si el usuario cancela la confirmación', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<AdminImagesPage />)
    const eliminarBtns = await screen.findAllByRole('button', { name: /Eliminar/i })

    await user.click(eliminarBtns[0])

    expect(mockDeleteAdminImagen).not.toHaveBeenCalled()
    confirmSpy.mockRestore()
  })
})
