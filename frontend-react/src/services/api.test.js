import api, {
  getAdminReservas,
  getAdminReservaDetalle,
  putAdminReserva,
  patchAdminReservaEstado,
  deleteAdminReserva,
  getAdminMensajes,
  patchAdminMensajeLeido,
  deleteAdminMensaje,
  getAdminImagenes,
  uploadAdminImagen,
  patchAdminImagenOrden,
  deleteAdminImagen,
  getAdminTextos,
  getTextoLegal,
  putTextoLegal,
} from './api';
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

describe('api helpers de reservas y mensajes', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminReservas llama a GET /admin/reservas con estado', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminReservas('aprobada')
    expect(spy).toHaveBeenCalledWith('/admin/reservas', expect.objectContaining({ params: { estado: 'aprobada' } }))
    spy.mockRestore()
  })

  it('getAdminReservas sin estado omite el param', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminReservas(undefined)
    expect(spy).toHaveBeenCalledWith('/admin/reservas', expect.objectContaining({ params: {} }))
    spy.mockRestore()
  })

  it('patchAdminReservaEstado llama a PATCH con el estado', async () => {
    const spy = vi.spyOn(api, 'patch').mockResolvedValue({ data: {} })
    await patchAdminReservaEstado(1, 'aprobada')
    expect(spy).toHaveBeenCalledWith('/admin/reservas/1/estado', { estado: 'aprobada' })
    spy.mockRestore()
  })

  it('getAdminMensajes sin filtro no envía leido', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminMensajes(undefined)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes', expect.objectContaining({ params: {} }))
    spy.mockRestore()
  })

  it('getAdminMensajes con filtro envía leido', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminMensajes(true)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes', expect.objectContaining({ params: { leido: true } }))
    spy.mockRestore()
  })

  it('patchAdminMensajeLeido llama a PATCH con leido', async () => {
    const spy = vi.spyOn(api, 'patch').mockResolvedValue({ data: {} })
    await patchAdminMensajeLeido(5, true)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes/5/leido', { leido: true })
    spy.mockRestore()
  })

  it('deleteAdminMensaje llama a DELETE', async () => {
    const spy = vi.spyOn(api, 'delete').mockResolvedValue({ data: {} })
    await deleteAdminMensaje(5)
    expect(spy).toHaveBeenCalledWith('/admin/mensajes/5')
    spy.mockRestore()
  })
})

describe('api helpers de imagenes y textos legales', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminImagenes llama a GET /admin/imagenes', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminImagenes()
    expect(spy).toHaveBeenCalledWith('/admin/imagenes')
    spy.mockRestore()
  })

  it('uploadAdminImagen llama a POST /admin/imagenes con FormData', async () => {
    const spy = vi.spyOn(api, 'post').mockResolvedValue({ data: {} })
    const fd = new FormData()
    await uploadAdminImagen(fd)
    expect(spy).toHaveBeenCalledWith('/admin/imagenes', fd)
    spy.mockRestore()
  })

  it('patchAdminImagenOrden llama a PATCH /admin/imagenes/:id/orden', async () => {
    const spy = vi.spyOn(api, 'patch').mockResolvedValue({ data: {} })
    await patchAdminImagenOrden(3, 5)
    expect(spy).toHaveBeenCalledWith('/admin/imagenes/3/orden', { orden: 5 })
    spy.mockRestore()
  })

  it('deleteAdminImagen llama a DELETE /admin/imagenes/:id', async () => {
    const spy = vi.spyOn(api, 'delete').mockResolvedValue({ data: {} })
    await deleteAdminImagen(3)
    expect(spy).toHaveBeenCalledWith('/admin/imagenes/3')
    spy.mockRestore()
  })

  it('getAdminTextos llama a GET /admin/textos', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    await getAdminTextos()
    expect(spy).toHaveBeenCalledWith('/admin/textos')
    spy.mockRestore()
  })

  it('getTextoLegal llama a GET /admin/textos/:slug', async () => {
    const spy = vi.spyOn(api, 'get').mockResolvedValue({ data: {} })
    await getTextoLegal('aviso-legal')
    expect(spy).toHaveBeenCalledWith('/admin/textos/aviso-legal')
    spy.mockRestore()
  })

  it('putTextoLegal llama a PUT /admin/textos/:slug', async () => {
    const spy = vi.spyOn(api, 'put').mockResolvedValue({ data: {} })
    await putTextoLegal('aviso-legal', { titulo: 'T', contenido: 'C' })
    expect(spy).toHaveBeenCalledWith('/admin/textos/aviso-legal', { titulo: 'T', contenido: 'C' })
    spy.mockRestore()
  })
})

