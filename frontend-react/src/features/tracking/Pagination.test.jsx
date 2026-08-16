import { render, screen, fireEvent } from '@testing-library/react'
import Pagination from './Pagination'

describe('Pagination', () => {
  const onChange = vi.fn()
  const onPageSizeChange = vi.fn()

  beforeEach(() => {
    onChange.mockClear()
    onPageSizeChange.mockClear()
  })

  it('no renderiza nada cuando no hay elementos', () => {
    const { container } = render(
      <Pagination page={0} totalPages={0} totalElements={0} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    expect(container).toBeEmptyDOMElement()
  })

  it('muestra el rango de elementos', () => {
    render(
      <Pagination page={0} totalPages={6} totalElements={57} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    expect(screen.getByText('1–10 de 57')).toBeInTheDocument()
  })

  it('muestra el rango correcto en la última página', () => {
    render(
      <Pagination page={5} totalPages={6} totalElements={57} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    expect(screen.getByText('51–57 de 57')).toBeInTheDocument()
  })

  it('renderiza el selector de tamaño y notifica cambios', () => {
    render(
      <Pagination page={0} totalPages={1} totalElements={5} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    const select = screen.getByLabelText('Resultados por página')
    expect(select).toBeInTheDocument()
    fireEvent.change(select, { target: { value: '25' } })
    expect(onPageSizeChange).toHaveBeenCalledWith(25)
  })

  it('deshabilita Anterior en la primera página', () => {
    render(
      <Pagination page={0} totalPages={3} totalElements={30} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    expect(screen.getByText('‹').closest('button')).toBeDisabled()
  })

  it('deshabilita Siguiente en la última página', () => {
    render(
      <Pagination page={2} totalPages={3} totalElements={30} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    expect(screen.getByText('›').closest('button')).toBeDisabled()
  })

  it('navega al hacer click en un número de página', () => {
    render(
      <Pagination page={0} totalPages={3} totalElements={30} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    fireEvent.click(screen.getByText('2'))
    expect(onChange).toHaveBeenCalledWith(1)
  })

  it('navega con Siguiente', () => {
    render(
      <Pagination page={0} totalPages={3} totalElements={30} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    fireEvent.click(screen.getByText('›'))
    expect(onChange).toHaveBeenCalledWith(1)
  })

  it('navega con Anterior', () => {
    render(
      <Pagination page={2} totalPages={3} totalElements={30} pageSize={10} onChange={onChange} onPageSizeChange={onPageSizeChange} />,
    )
    fireEvent.click(screen.getByText('‹'))
    expect(onChange).toHaveBeenCalledWith(1)
  })
})
