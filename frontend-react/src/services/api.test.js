import { getDocumentoUrl, descargarDocumento, formatPesoBytes } from './api'

describe('api helpers de documentos', () => {
  it('construye la URL de la etiqueta de un envío', () => {
    expect(getDocumentoUrl('etiqueta', 'MT-0001')).toBe('/admin/documentos/envios/MT-0001/etiqueta')
  })

  it('construye la URL de etiquetas de lote', () => {
    expect(getDocumentoUrl('etiquetas-lote', 10)).toBe('/admin/documentos/lotes/10/etiquetas')
  })

  it('construye la URL del manifiesto de lote', () => {
    expect(getDocumentoUrl('manifiesto', 10)).toBe('/admin/documentos/lotes/10/manifiesto')
  })

  it('descargarDocumento crea un anchor con href y download, hace click y lo elimina', () => {
    const appendSpy = vi.spyOn(document.body, 'appendChild')
    const removeSpy = vi.spyOn(document.body, 'removeChild')
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click')
    const url = '/admin/documentos/lotes/10/manifiesto'

    descargarDocumento(url)

    const anchor = appendSpy.mock.calls[0][0]
    expect(anchor.tagName).toBe('A')
    expect(anchor.href).toContain(url)
    expect(anchor.download).toBe('')
    expect(clickSpy).toHaveBeenCalledTimes(1)
    expect(removeSpy).toHaveBeenCalledTimes(1)
    expect(removeSpy.mock.calls[0][0]).toBe(anchor)
  })

  it('formatea bytes a KB con 1 decimal', () => {
    expect(formatPesoBytes(1536)).toBe('1.5 KB')
    expect(formatPesoBytes(0)).toBe('0.0 KB')
  })
})
