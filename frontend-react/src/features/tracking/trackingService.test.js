import { buildTrackingParams, fetchEnvios, deleteEnvio } from './trackingService'
import { getAdminEnvios, deleteAdminEnvio } from '../../services/api'

vi.mock('../../services/api', () => ({
  getAdminEnvios: vi.fn(),
  deleteAdminEnvio: vi.fn(),
}))

describe('trackingService', () => {
  describe('buildTrackingParams', () => {
    it('devuelve page y size por defecto', () => {
      expect(buildTrackingParams()).toEqual({ page: 0, size: 10 })
    })

    it('incluye page y size personalizados', () => {
      expect(buildTrackingParams({ page: 2, size: 25 })).toEqual({ page: 2, size: 25 })
    })

    it('mapea query a q y fechas', () => {
      expect(
        buildTrackingParams({ query: 'ana', fechaDesde: '2026-08-01', fechaHasta: '2026-08-15' }),
      ).toEqual({ page: 0, size: 10, q: 'ana', fechaDesde: '2026-08-01', fechaHasta: '2026-08-15' })
    })

    it('incluye estados como array cuando hay selección', () => {
      expect(buildTrackingParams({ estados: ['RECIBIDO', 'ENTREGADO'] })).toEqual({
        page: 0,
        size: 10,
        estados: ['RECIBIDO', 'ENTREGADO'],
      })
    })

    it('omite filtros vacíos', () => {
      expect(buildTrackingParams({ query: '', fechaDesde: '', fechaHasta: '', estados: [] })).toEqual({
        page: 0,
        size: 10,
      })
    })

    it('incluye sort cuando se pasa', () => {
      expect(buildTrackingParams({ sort: 'ultimaActualizacion,desc' })).toEqual({
        page: 0,
        size: 10,
        sort: 'ultimaActualizacion,desc',
      })
    })
  })

  describe('fetchEnvios', () => {
    it('llama getAdminEnvios con los parámetros construidos', async () => {
      getAdminEnvios.mockResolvedValue({ data: { content: [] } })
      await fetchEnvios({ page: 1, estados: ['ENTREGADO'] })
      expect(getAdminEnvios).toHaveBeenCalledWith({ page: 1, size: 10, estados: ['ENTREGADO'] })
    })
  })

  describe('deleteEnvio', () => {
    it('llama deleteAdminEnvio con el código', async () => {
      deleteAdminEnvio.mockResolvedValue({})
      await deleteEnvio('MT-0001')
      expect(deleteAdminEnvio).toHaveBeenCalledWith('MT-0001')
    })
  })
})
