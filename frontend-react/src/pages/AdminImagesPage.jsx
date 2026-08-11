import { useState, useEffect, useCallback } from 'react'
import {
  getAdminImagenes,
  uploadAdminImagen,
  patchAdminImagenOrden,
  deleteAdminImagen,
} from '../services/api'
import { useToast } from '../context/NotificationContext'
import EmptyState from '../components/EmptyState'

export default function AdminImagesPage() {
  const [imagenes, setImagenes] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const { showSuccess, showError } = useToast()

  // Form state
  const [titulo, setTitulo] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [categoria, setCategoria] = useState('')
  const [orden, setOrden] = useState(0)
  const [file, setFile] = useState(null)

  const cargarImagenes = useCallback(async () => {
    try {
      setLoading(true)
      const res = await getAdminImagenes()
      setImagenes(res.data || [])
    } catch (err) {
      showError(err.message || 'Error al cargar las imágenes de la galería')
    } finally {
      setLoading(false)
    }
  }, [showError])

  useEffect(() => {
    cargarImagenes()
  }, [cargarImagenes])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!titulo || !titulo.trim()) {
      showError('Debes ingresar un título')
      return
    }
    if (!file) {
      showError('Debes seleccionar un archivo de imagen')
      return
    }

    try {
      setUploading(true)
      const formData = new FormData()
      formData.append('archivo', file)
      formData.append('titulo', titulo)
      if (descripcion) formData.append('descripcion', descripcion)
      if (categoria) formData.append('categoria', categoria)
      formData.append('orden', orden)

      await uploadAdminImagen(formData)
      showSuccess('Imagen subida correctamente')

      // Limpiar formulario
      setTitulo('')
      setDescripcion('')
      setCategoria('')
      setOrden(0)
      setFile(null)
      e.target.reset()

      await cargarImagenes()
    } catch (err) {
      showError(err.message || 'Error al subir la imagen')
    } finally {
      setUploading(false)
    }
  }

  const handleOrdenChange = (id, val) => {
    setImagenes((prev) =>
      prev.map((img) => (img.id === id ? { ...img, orden: Number(val) } : img))
    )
  }

  const handleOrdenBlur = async (id, val) => {
    try {
      await patchAdminImagenOrden(id, Number(val))
      showSuccess('Orden actualizado')
    } catch (err) {
      showError(err.message || 'Error al actualizar el orden')
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('¿Seguro que deseas eliminar esta imagen?')) return
    try {
      await deleteAdminImagen(id)
      showSuccess('Imagen eliminada correctamente')
      await cargarImagenes()
    } catch (err) {
      showError(err.message || 'Error al eliminar la imagen')
    }
  }

  return (
    <div className="admin-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Imágenes de la galería</h1>
          <p className="page-subtitle">
            Gestión de imágenes de la web y documentación.
          </p>
        </div>
      </div>

      {/* Formulario de subida */}
      <div className="card upload-card mb-6">
        <h2 className="card-title mb-4">Subir nueva imagen</h2>
        <form onSubmit={handleSubmit} className="upload-form">
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="titulo">Título *</label>
              <input
                id="titulo"
                type="text"
                className="input"
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="archivo">Archivo de imagen *</label>
              <input
                id="archivo"
                type="file"
                className="input"
                accept="image/*"
                onChange={(e) => setFile(e.target.files[0] || null)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="categoria">Categoría</label>
              <input
                id="categoria"
                type="text"
                className="input"
                placeholder="Ej: banner, logos..."
                value={categoria}
                onChange={(e) => setCategoria(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="orden">Orden *</label>
              <input
                id="orden"
                type="number"
                className="input"
                value={orden}
                onChange={(e) => setOrden(Number(e.target.value))}
              />
            </div>
          </div>

          <div className="form-group mt-4">
            <label htmlFor="descripcion">Descripción</label>
            <textarea
              id="descripcion"
              className="input textarea"
              rows="2"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
            />
          </div>

          <div className="form-actions mt-4">
            <button
              type="submit"
              className="btn btn-luxury"
              disabled={uploading}
            >
              {uploading ? 'Subiendo...' : 'Subir imagen'}
            </button>
          </div>
        </form>
      </div>

      {/* Rejilla de galería */}
      {loading ? (
        <div className="loading-container">Cargando galería...</div>
      ) : imagenes.length === 0 ? (
        <EmptyState message="No hay imágenes en la galería" />
      ) : (
        <div className="galeria-grid">
          {imagenes.map((img) => (
            <div key={img.id} className="card imagen-card">
              <div className="imagen-thumb-container">
                <img
                  src={img.url}
                  alt={img.titulo}
                  className="imagen-card-thumb"
                />
              </div>
              <div className="imagen-card-body">
                <div className="imagen-card-header">
                  <h3 className="imagen-card-title">{img.titulo}</h3>
                  {img.categoria && (
                    <span className="badge badge-cat">{img.categoria}</span>
                  )}
                </div>
                {img.descripcion && (
                  <p className="imagen-card-desc">{img.descripcion}</p>
                )}

                <div className="imagen-card-footer">
                  <div className="orden-control">
                    <label htmlFor={`orden-${img.id}`}>Orden:</label>
                    <input
                      id={`orden-${img.id}`}
                      aria-label="Orden de la imagen"
                      type="number"
                      className="input input-sm orden-input"
                      value={img.orden}
                      onChange={(e) =>
                        handleOrdenChange(img.id, e.target.value)
                      }
                      onBlur={(e) =>
                        handleOrdenBlur(img.id, e.target.value)
                      }
                    />
                  </div>

                  <button
                    type="button"
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(img.id)}
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
