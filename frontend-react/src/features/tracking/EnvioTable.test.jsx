import { render, screen, fireEvent, within } from '@testing-library/react'
import EnvioTable from './EnvioTable'

const envios = [
  { codigoUnico: 'MT-0001', estado: 'EN_TRANSITO', destinatario: 'Ana', origen: 'Madrid', destino: 'Asunción', ultimaActualizacion: '2026-08-10T12:00:00' },
  { codigoUnico: 'MT-0002', estado: 'ENTREGADO', destinatario: 'Luis', origen: 'Barcelona', destino: 'Asunción', ultimaActualizacion: '2026-08-09T12:00:00' },
]

const baseProps = {
  envios,
  loading: false,
  page: 0,
  totalPages: 1,
  totalElements: 2,
  pageSize: 10,
  hasFilters: false,
  onPageChange: vi.fn(),
  onPageSizeChange: vi.fn(),
  onEdit: vi.fn(),
  onDelete: vi.fn(),
  onRowClick: vi.fn(),
}

describe('EnvioTable', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('renderiza filas con código, destinatario, estado y fecha', () => {
    render(<EnvioTable {...baseProps} />)
    expect(screen.getByText('MT-0001')).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('EN TRANSITO')).toBeInTheDocument()
    const expected = new Date('2026-08-10T12:00:00').toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })
    expect(screen.getByText(expected)).toBeInTheDocument()
  })

  it('muestra el contador de registros', () => {
    render(<EnvioTable {...baseProps} />)
    expect(screen.getByText('2 registros')).toBeInTheDocument()
  })

  it('muestra TableSkeleton cuando loading', () => {
    const { container } = render(<EnvioTable {...baseProps} loading />)
    expect(container.querySelector('[data-testid="table-skeleton"]')).toBeInTheDocument()
  })

  it('muestra EmptyState sin filtros activos', () => {
    render(<EnvioTable {...baseProps} envios={[]} totalElements={0} />)
    expect(screen.getByText('No hay envíos registrados')).toBeInTheDocument()
  })

  it('muestra EmptyState con filtros activos', () => {
    render(<EnvioTable {...baseProps} envios={[]} totalElements={0} hasFilters />)
    expect(screen.getByText('No se encontraron envíos con esos filtros')).toBeInTheDocument()
  })

  it('navega al detalle al hacer click en la fila', () => {
    render(<EnvioTable {...baseProps} />)
    fireEvent.click(screen.getByText('MT-0002'))
    expect(baseProps.onRowClick).toHaveBeenCalledWith('MT-0002')
  })

  it('Editar llama onEdit y no dispara onRowClick', () => {
    render(<EnvioTable {...baseProps} />)
    const firstRow = screen.getAllByRole('row')[1]
    fireEvent.click(within(firstRow).getByText('Editar'))
    expect(baseProps.onEdit).toHaveBeenCalledWith('MT-0001')
    expect(baseProps.onRowClick).not.toHaveBeenCalled()
  })

  it('Eliminar llama onDelete con el envío y no dispara onRowClick', () => {
    render(<EnvioTable {...baseProps} />)
    const firstRow = screen.getAllByRole('row')[1]
    fireEvent.click(within(firstRow).getByText('Eliminar'))
    expect(baseProps.onDelete).toHaveBeenCalledWith(envios[0])
    expect(baseProps.onRowClick).not.toHaveBeenCalled()
  })

  it('renderiza la paginación y notifica cambio de tamaño', () => {
    render(<EnvioTable {...baseProps} totalPages={3} totalElements={30} />)
    fireEvent.change(screen.getByLabelText('Resultados por página'), { target: { value: '25' } })
    expect(baseProps.onPageSizeChange).toHaveBeenCalledWith(25)
  })
})
