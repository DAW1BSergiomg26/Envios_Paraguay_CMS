import { render, screen, fireEvent } from '@testing-library/react'
import EnvioFilters from './EnvioFilters'

describe('EnvioFilters', () => {
  const handlers = {
    onQueryChange: vi.fn(),
    onEstadosChange: vi.fn(),
    onFechaDesdeChange: vi.fn(),
    onFechaHastaChange: vi.fn(),
    onRemoveEstado: vi.fn(),
    onClearQuery: vi.fn(),
    onClearFecha: vi.fn(),
    onClearAll: vi.fn(),
  }

  const baseProps = {
    query: '',
    estados: [],
    fechaDesde: '',
    fechaHasta: '',
    ...handlers,
  }

  beforeEach(() => {
    Object.values(handlers).forEach((fn) => fn.mockClear())
  })

  it('renderiza búsqueda, rango de fechas y los 6 chips de estado', () => {
    render(<EnvioFilters {...baseProps} />)
    expect(screen.getByPlaceholderText('Buscar por código, cliente o destinatario...')).toBeInTheDocument()
    expect(screen.getByTitle('Fecha desde')).toBeInTheDocument()
    expect(screen.getByTitle('Fecha hasta')).toBeInTheDocument()
    expect(screen.getByText('Recibido')).toBeInTheDocument()
    expect(screen.getByText('En Tránsito')).toBeInTheDocument()
    expect(screen.getByText('Entregado')).toBeInTheDocument()
  })

  it('notifica el toggle de un estado', () => {
    render(<EnvioFilters {...baseProps} />)
    fireEvent.click(screen.getByText('Entregado'))
    expect(handlers.onEstadosChange).toHaveBeenCalledWith(['ENTREGADO'])
  })

  it('propaga cambios de fecha', () => {
    render(<EnvioFilters {...baseProps} />)
    fireEvent.change(screen.getByTitle('Fecha desde'), { target: { value: '2026-08-01' } })
    expect(handlers.onFechaDesdeChange).toHaveBeenCalledWith('2026-08-01')
  })

  it('muestra filtros activos y permite limpiarlos', () => {
    render(<EnvioFilters {...baseProps} query="ana" estados={['ENTREGADO']} />)
    expect(screen.getByText('"ana"')).toBeInTheDocument()
    expect(screen.getAllByText('Entregado')).toHaveLength(2)
    fireEvent.click(screen.getByText('Limpiar filtros'))
    expect(handlers.onClearAll).toHaveBeenCalled()
  })
})
