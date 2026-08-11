import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  postAdminEnvio,
  putAdminEnvio,
  getAdminEnvioDetalle,
  getAdminClientes,
} from '../services/api'
import { useToast } from '../context/NotificationContext'

const ESTADOS = [
  'RECIBIDO',
  'EN_ADUANA_ORIGEN',
  'EN_TRANSITO',
  'EN_ADUANA_DESTINO',
  'EN_REPARTO',
  'ENTREGADO',
]

const formatearEstado = (estado) => estado.replace(/_/g, ' ')

export default function EnvioFormPage() {
  const { codigo } = useParams()
  const navigate = useNavigate()
  const { showSuccess, showError } = useToast()

  const esEdicion = Boolean(codigo)

  const [loading, setLoading] = useState(esEdicion)
  const [saving, setSaving] = useState(false)
  const [clientes, setClientes] = useState([])
  const [error, setError] = useState(null)

  const [form, setForm] = useState({
    codigoUnico: '',
    estado: 'RECIBIDO',
    destinatario: '',
    origen: '',
    destino: '',
    peso: '',
    contenido: '',
    observaciones: '',
    clienteId: '',
  })
  const [errores, setErrores] = useState({})

  useEffect(() => {
    getAdminClientes()
      .then((res) => setClientes(res.data || []))
      .catch(() => setClientes([]))
  }, [])

  useEffect(() => {
    if (!esEdicion) return
    getAdminEnvioDetalle(codigo)
      .then((res) => {
        const envio = res.data
        setForm({
          codigoUnico: envio.codigoUnico || '',
          estado: envio.estado || 'RECIBIDO',
          destinatario: envio.destinatario || '',
          origen: envio.origen || '',
          destino: envio.destino || '',
          peso: envio.peso || '',
          contenido: envio.contenido || '',
          observaciones: envio.observaciones || '',
          clienteId: envio.clienteId ? String(envio.clienteId) : '',
        })
      })
      .catch((err) => {
        setError(err.message || 'Error al cargar el envío')
        showError(err.message || 'Error al cargar el envío')
      })
      .finally(() => setLoading(false))
  }, [codigo, esEdicion, showError])

  const handleChange = (campo) => (e) => {
    setForm((prev) => ({ ...prev, [campo]: e.target.value }))
    setErrores((prev) => ({ ...prev, [campo]: undefined }))
  }

  const validar = () => {
    const nuevosErrores = {}
    if (!form.destinatario.trim()) {
      nuevosErrores.destinatario = 'Destinatario es obligatorio'
    }
    setErrores(nuevosErrores)
    return Object.keys(nuevosErrores).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validar()) return

    const body = {
      ...form,
      clienteId: form.clienteId ? Number(form.clienteId) : null,
    }

    try {
      setSaving(true)
      if (esEdicion) {
        const { codigoUnico, ...resto } = body
        const res = await putAdminEnvio(codigo, resto)
        showSuccess('Envío actualizado correctamente')
        navigate(`/dashboard/envio/${res.data.codigoUnico || codigo}`)
      } else {
        const res = await postAdminEnvio(body)
        showSuccess('Envío creado correctamente')
        navigate(`/dashboard/envio/${res.data.codigoUnico}`)
      }
    } catch (err) {
      showError(err.message || 'Error al guardar el envío')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <div className="loading-container">Cargando envío...</div>
  }

  return (
    <div className="admin-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">
            {esEdicion ? 'Editar envío' : 'Nuevo envío'}
          </h1>
          <p className="page-subtitle">
            {esEdicion
              ? `Modifica los datos del envío ${codigo}.`
              : 'Registra un nuevo envío en el sistema.'}
          </p>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="envio-form card" noValidate>
        <div className="envio-form-grid grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="form-group">
            <label htmlFor="envio-codigo" className="label font-medium block mb-1">
              Código
            </label>
            <input
              id="envio-codigo"
              type="text"
              className="input w-full"
              placeholder={
                esEdicion ? form.codigoUnico : 'Se generará automáticamente'
              }
              value={form.codigoUnico}
              onChange={handleChange('codigoUnico')}
              disabled={esEdicion}
            />
          </div>

          <div className="form-group">
            <label htmlFor="envio-estado" className="label font-medium block mb-1">
              Estado *
            </label>
            <select
              id="envio-estado"
              className="input w-full"
              value={form.estado}
              onChange={handleChange('estado')}
            >
              {ESTADOS.map((estado) => (
                <option key={estado} value={estado}>
                  {formatearEstado(estado)}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="envio-destinatario" className="label font-medium block mb-1">
              Destinatario *
            </label>
            <input
              id="envio-destinatario"
              type="text"
              className="input w-full"
              value={form.destinatario}
              onChange={handleChange('destinatario')}
              aria-invalid={Boolean(errores.destinatario)}
            />
            {errores.destinatario && (
              <span className="form-error text-sm text-red-500" role="alert">
                {errores.destinatario}
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="envio-cliente" className="label font-medium block mb-1">
              Cliente
            </label>
            <select
              id="envio-cliente"
              className="input w-full"
              value={form.clienteId}
              onChange={handleChange('clienteId')}
            >
              <option value="">Sin asignar</option>
              {clientes.map((cliente) => (
                <option key={cliente.id} value={cliente.id}>
                  {cliente.nombre}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="envio-origen" className="label font-medium block mb-1">
              Origen
            </label>
            <input
              id="envio-origen"
              type="text"
              className="input w-full"
              value={form.origen}
              onChange={handleChange('origen')}
            />
          </div>

          <div className="form-group">
            <label htmlFor="envio-destino" className="label font-medium block mb-1">
              Destino
            </label>
            <input
              id="envio-destino"
              type="text"
              className="input w-full"
              value={form.destino}
              onChange={handleChange('destino')}
            />
          </div>

          <div className="form-group">
            <label htmlFor="envio-peso" className="label font-medium block mb-1">
              Peso
            </label>
            <input
              id="envio-peso"
              type="text"
              className="input w-full"
              value={form.peso}
              onChange={handleChange('peso')}
            />
          </div>

          <div className="form-group">
            <label htmlFor="envio-contenido" className="label font-medium block mb-1">
              Contenido
            </label>
            <input
              id="envio-contenido"
              type="text"
              className="input w-full"
              value={form.contenido}
              onChange={handleChange('contenido')}
            />
          </div>

          <div className="form-group md:col-span-2">
            <label htmlFor="envio-observaciones" className="label font-medium block mb-1">
              Observaciones
            </label>
            <textarea
              id="envio-observaciones"
              className="input textarea w-full"
              rows="3"
              value={form.observaciones}
              onChange={handleChange('observaciones')}
            />
          </div>
        </div>

        <div className="form-actions pt-4 border-t flex justify-end gap-3">
          <button
            type="button"
            className="btn"
            onClick={() => navigate('/')}
            disabled={saving}
          >
            Cancelar
          </button>
          <button type="submit" className="btn btn-luxury" disabled={saving}>
            {saving ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  )
}
