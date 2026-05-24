import { render, screen } from '@testing-library/react'
import StatusBadge from './StatusBadge'

describe('StatusBadge', () => {
  it('renderiza estado EN_TRANSITO formateado', () => {
    render(<StatusBadge estado="EN_TRANSITO" />)
    expect(screen.getByText('EN TRANSITO')).toBeInTheDocument()
  })

  it('renderiza estado ENTREGADO', () => {
    render(<StatusBadge estado="ENTREGADO" />)
    expect(screen.getByText('ENTREGADO')).toBeInTheDocument()
  })

  it('renderiza N/A cuando estado es null', () => {
    render(<StatusBadge estado={null} />)
    expect(screen.getByText('N/A')).toBeInTheDocument()
  })
})
