import { render, screen } from '@testing-library/react'
import AppleHero from './AppleHero'

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}))

vi.mock('../hooks/useAppleScroll', () => ({
  useAppleScroll: vi.fn(),
}))

describe('AppleHero', () => {
  it('renderiza título, subtítulo y acciones', () => {
    render(<AppleHero />)
    expect(screen.getByRole('heading', { name: /gestiona tus envíos/i })).toBeInTheDocument()
    expect(screen.getByText(/visión operativa en tiempo real/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /crear envío/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /ver métricas/i })).toBeInTheDocument()
  })

  it('renderiza las tarjetas de características', () => {
    render(<AppleHero />)
    expect(screen.getByText('Tiempo real')).toBeInTheDocument()
    expect(screen.getByText('Control por roles')).toBeInTheDocument()
    expect(screen.getByText('Operaciones ágiles')).toBeInTheDocument()
  })
})
