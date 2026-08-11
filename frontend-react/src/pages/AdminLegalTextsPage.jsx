import { useState, useEffect, useCallback } from 'react'
import {
  getAdminTextos,
  getTextoLegal,
  putTextoLegal,
} from '../services/api'
import { useToast } from '../context/NotificationContext'

export default function AdminLegalTextsPage() {
  const [textos, setTextos] = useState([])
  const [loadingList, setLoadingList] = useState(true)

  const [selectedSlug, setSelectedSlug] = useState(null)
  const [detalle, setDetalle] = useState(null)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [saving, setSaving] = useState(false)

  const [formTitulo, setFormTitulo] = useState('')
  const [formContenido, setFormContenido] = useState('')

  const { showSuccess, showError } = useToast()

  const cargarTextos = useCallback(async () => {
    try {
      setLoadingList(true)
      const res = await getAdminTextos()
      const list = res.data || []
      setTextos(list)
    } catch (err) {
      showError(err.message || 'Error al cargar los textos legales')
    } finally {
      setLoadingList(false)
    }
  }, [showError])

  useEffect(() => {
    cargarTextos()
  }, [cargarTextos])

  const cargarDetalle = useCallback(
    async (slug) => {
      try {
        setLoadingDetail(true)
        const res = await getTextoLegal(slug)
        const item = res.data
        setDetalle(item)
        setFormTitulo(item?.titulo || '')
        setFormContenido(item?.contenido || '')
      } catch (err) {
        showError(err.message || 'Error al cargar el detalle del texto legal')
      } finally {
        setLoadingDetail(false)
      }
    },
    [showError]
  )

  useEffect(() => {
    if (selectedSlug) {
      cargarDetalle(selectedSlug)
    } else {
      setDetalle(null)
      setFormTitulo('')
      setFormContenido('')
    }
  }, [selectedSlug, cargarDetalle])

  const handleSelect = (slug) => {
    setSelectedSlug(slug)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    if (!selectedSlug) return

    if (!formTitulo.trim() || !formContenido.trim()) {
      showError('Título y contenido son requeridos')
      return
    }

    try {
      setSaving(true)
      const res = await putTextoLegal(selectedSlug, {
        titulo: formTitulo,
        contenido: formContenido,
      })
      showSuccess('Texto legal guardado correctamente')
      setDetalle(res.data)
      await cargarTextos()
    } catch (err) {
      showError(err.message || 'Error al guardar el texto legal')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="admin-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Textos legales</h1>
          <p className="page-subtitle">
            Gestión de aviso legal y política de cookies.
          </p>
        </div>
      </div>

      <div className="textos-layout grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Columna izquierda: Lista Master */}
        <div className="card master-card md:col-span-1">
          <h2 className="card-title mb-4">Documentos</h2>
          {loadingList ? (
            <div className="loading-container">Cargando lista...</div>
          ) : textos.length === 0 ? (
            <div className="empty-textos p-4 text-center text-muted">
              No hay textos legales registrados.
            </div>
          ) : (
            <div className="textos-master-list space-y-2">
              {textos.map((t) => {
                const isSelected = selectedSlug === t.slug
                return (
                  <button
                    key={t.id}
                    type="button"
                    className={`master-item w-full text-left p-3 rounded-md transition-colors ${
                      isSelected
                        ? 'master-item--selected bg-primary text-white font-semibold'
                        : 'hover:bg-surface-hover'
                    }`}
                    onClick={() => handleSelect(t.slug)}
                  >
                    <div className="master-item-title font-medium">
                      {t.titulo}
                    </div>
                    <div className="master-item-slug text-xs opacity-75">
                      {t.slug}
                    </div>
                  </button>
                )
              })}
            </div>
          )}
        </div>

        {/* Columna derecha: Editor Detail */}
        <div className="card detail-card md:col-span-2">
          {!selectedSlug ? (
            <div className="empty-selection p-8 text-center text-muted">
              Selecciona un texto legal para editar
            </div>
          ) : loadingDetail ? (
            <div className="loading-container p-8 text-center">
              Cargando contenido...
            </div>
          ) : (
            <form onSubmit={handleSave} className="texto-editor space-y-4">
              <div className="detail-header flex items-center justify-between border-b pb-3 mb-4">
                <div>
                  <h2 className="text-xl font-semibold">Editar documento</h2>
                  <span className="badge badge-slug text-xs">{detalle?.slug}</span>
                </div>
                {detalle?.updatedAt && (
                  <span className="text-xs text-muted">
                    Última actualización:{' '}
                    {new Date(detalle.updatedAt).toLocaleString()}
                  </span>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="titulo-editor" className="label font-medium block mb-1">
                  Título *
                </label>
                <input
                  id="titulo-editor"
                  type="text"
                  className="input w-full"
                  value={formTitulo}
                  onChange={(e) => setFormTitulo(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label htmlFor="contenido-editor" className="label font-medium block mb-1">
                  Contenido *
                </label>
                <textarea
                  id="contenido-editor"
                  className="input textarea w-full font-mono text-sm"
                  rows="14"
                  value={formContenido}
                  onChange={(e) => setFormContenido(e.target.value)}
                />
              </div>

              <div className="form-actions pt-4 border-t flex justify-end">
                <button
                  type="submit"
                  className="btn btn-luxury"
                  disabled={saving}
                >
                  {saving ? 'Guardando...' : 'Guardar cambios'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
