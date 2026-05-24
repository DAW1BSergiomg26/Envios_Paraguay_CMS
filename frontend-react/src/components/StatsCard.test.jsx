import { render, screen } from '@testing-library/react'
import StatsCard from './StatsCard'

describe('StatsCard', () => {
  it('renderiza label y valor', () => {
    render(<StatsCard label="Envíos totales" value="42" />)
    expect(screen.getByText('Envíos totales')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('renderiza icono y color cuando se pasan', () => {
    render(<StatsCard label="Activos" value="12" icon="🚀" color="#3b82f6" />)
    expect(screen.getByText('🚀')).toBeInTheDocument()
    expect(screen.getByText('Activos')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
  })
})
