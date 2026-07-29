import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import LoginPage from '../pages/LoginPage'

const mockLogin = vi.fn()
const mockNavigate = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({ state: null }),
  }
})

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ login: mockLogin, user: null, loading: false }),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza formulario con campos usuario y password', () => {
    render(<LoginPage />)
    expect(screen.getByLabelText('Usuario')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument()
  })

  it('llama login al enviar formulario con credenciales correctas', async () => {
    mockLogin.mockResolvedValue(true)
    render(<LoginPage />)
    await userEvent.type(screen.getByLabelText('Usuario'), 'admin')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'pass123')
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }))
    expect(mockLogin).toHaveBeenCalledWith('admin', 'pass123')
  })

  it('login exitoso navega a home', async () => {
    mockLogin.mockResolvedValue(true)
    render(<LoginPage />)
    await userEvent.type(screen.getByLabelText('Usuario'), 'admin')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'pass123')
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }))
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })

  it('login fallido muestra error', async () => {
    mockLogin.mockResolvedValue(false)
    render(<LoginPage />)
    await userEvent.type(screen.getByLabelText('Usuario'), 'bad')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }))
    expect(await screen.findByText('Credenciales incorrectas')).toBeInTheDocument()
  })
})
