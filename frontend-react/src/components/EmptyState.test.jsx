import { render, screen } from '@testing-library/react'
import EmptyState from './EmptyState'

describe('EmptyState', () => {
  it('renderiza mensaje por defecto', () => {
    render(<EmptyState />)
    expect(screen.getByText('Sin envíos encontrados')).toBeInTheDocument()
  })

  it('renderiza mensaje personalizado', () => {
    render(<EmptyState message="No hay resultados para tu búsqueda" />)
    expect(screen.getByText('No hay resultados para tu búsqueda')).toBeInTheDocument()
  })
})
