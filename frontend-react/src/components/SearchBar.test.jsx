import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import SearchBar from './SearchBar'

describe('SearchBar', () => {
  it('renderiza input con placeholder por defecto', () => {
    render(<SearchBar value="" onChange={() => {}} />)
    expect(screen.getByPlaceholderText('Buscar...')).toBeInTheDocument()
  })

  it('renderiza placeholder personalizado', () => {
    render(<SearchBar value="" onChange={() => {}} placeholder="Buscar envíos..." />)
    expect(screen.getByPlaceholderText('Buscar envíos...')).toBeInTheDocument()
  })

  it('llama onChange tras debounce de 300ms', async () => {
    const onChange = vi.fn()
    render(<SearchBar value="" onChange={onChange} />)
    const input = screen.getByPlaceholderText('Buscar...')
    fireEvent.change(input, { target: { value: 'test' } })
    expect(onChange).not.toHaveBeenCalled()
    await waitFor(() => expect(onChange).toHaveBeenCalledWith('test'), { timeout: 1000 })
  })

  it('limpia el input al hacer click en boton clear', () => {
    const onChange = vi.fn()
    render(<SearchBar value="test" onChange={onChange} />)
    const clearBtn = screen.getByText('✕')
    fireEvent.click(clearBtn)
    expect(onChange).toHaveBeenCalledWith('')
  })
})
