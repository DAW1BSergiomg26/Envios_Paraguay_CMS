import { useState, useEffect, useCallback, Fragment } from 'react'
import {
  getAdminClientes,
  listarWebhooks,
  crearWebhook,
  actualizarWebhook,
  eliminarWebhook,
  listarWebhookLogs,
} from '../services/api'
import { parseLocalDateTime } from '../services/dateUtils'
import { useToast } from '../context/NotificationContext'
import EmptyState from '../components/EmptyState'

const FORM_VACIO = { clienteId: '', url: '', secretToken: '', activo: true }

export default function WebhooksPage() {
  const { showSuccess, showError } = useToast()
  const [webhooks, setWebhooks] = useState([])
  const [clientes, setClientes] = useState([])
  const [filtroCliente, setFiltroCliente] = useState('')
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(FORM_VACIO)
  const [saving, setSaving] = useState(false)
  const [logsAbiertos, setLogsAbiertos] = useState({})
  const [logsPorWebhook, setLogsPorWebhook] = useState({})
  const [cargandoLogs, setCargandoLogs] = useState({})
  const [error, setError] = useState(null)

  const cargar = useCallback(async (filtro) => {
    try {
      setLoading(true)
      setError(null)
      const res = await listarWebhooks(filtro || undefined)
      setWebhooks(res.data || [])
    } catch (err) {
      setError(err.message || 'Error al cargar los webhooks')
      showError(err.message || 'Error al cargar los webhooks')
    } finally {
      setLoading(false)
    }
  }, [showError])

  const cargarClientes = useCallback(async () => {
    try {
      const res = await getAdminClientes()
      setClientes(res.data || [])
    } catch (err) {
      showError(err.message || 'Error al cargar los clientes')
    }
  }, [showError])

  useEffect(() => {
    cargarClientes()
    cargar(filtroCliente ? Number(filtroCliente) : undefined)
  }, [filtroCliente, cargar, cargarClientes])

  const abrirNuevo = () => {
    setEditing(null)
    setForm(FORM_VACIO)
    setShowForm(true)
  }

  const abrirEdicion = (webhook) => {
    setEditing(webhook)
    setForm({ clienteId: webhook.clienteId, url: webhook.url, secretToken: '', activo: webhook.activo })
    setShowForm(true)
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const payload = {
      url: form.url,
      activo: form.activo,
    }
    if (form.secretToken) payload.secretToken = form.secretToken
    if (!editing && form.clienteId) payload.clienteId = Number(form.clienteId)

    setSaving(true)
    try {
      if (editing) {
        await actualizarWebhook(editing.id, payload)
        showSuccess('Webhook actualizado')
      } else {
        await crearWebhook(payload)
        showSuccess('Webhook creado')
      }
      setShowForm(false)
      setEditing(null)
      setForm(FORM_VACIO)
      await cargar(filtroCliente)
    } catch (err) {
      showError(err.message || 'Error al guardar el webhook')
    } finally {
      setSaving(false)
    }
  }

  const eliminar = async (id) => {
    if (!window.confirm('¿Seguro que quieres eliminar este webhook?')) return
    try {
      await eliminarWebhook(id)
      showSuccess('Webhook eliminado')
      await cargar(filtroCliente)
    } catch (err) {
      showError(err.message || 'Error al eliminar el webhook')
    }
  }

  const alternarLogs = async (id) => {
    if (logsAbiertos[id]) {
      setLogsAbiertos((prev) => ({ ...prev, [id]: false }))
      return
    }
    if (!logsPorWebhook[id]) {
      setCargandoLogs((prev) => ({ ...prev, [id]: true }))
      try {
        const res = await listarWebhookLogs(id)
        setLogsPorWebhook((prev) => ({ ...prev, [id]: res.data || [] }))
      } catch (err) {
        showError(err.message || 'Error al cargar los logs del webhook')
      } finally {
        setCargandoLogs((prev) => ({ ...prev, [id]: false }))
      }
    }
    setLogsAbiertos((prev) => ({ ...prev, [id]: true }))
  }

  const nombreCliente = (id) => {
    const cliente = clientes.find((c) => c.id === id)
    return cliente ? cliente.nombre : `Cliente ${id}`
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Webhooks de clientes</h1>
          <p className="dashboard-subtitle">
            Configura los webhooks de notificación de estado hacia los clientes.
          </p>
        </div>
      </header>

      <div className="import-form-row">
        <div className="form-group mb-0">
          <label htmlFor="filtroCliente" className="label font-medium block mb-1">Filtrar por cliente</label>
          <select
            id="filtroCliente"
            className="import-select"
            value={filtroCliente}
            onChange={(e) => setFiltroCliente(e.target.value)}
          >
            <option value="">Todos los clientes</option>
            {clientes.map((c) => (
              <option key={c.id} value={c.id}>{c.nombre}</option>
            ))}
          </select>
        </div>
        <button className="btn btn-luxury" onClick={abrirNuevo}>Nuevo webhook</button>
      </div>

      {showForm && (
        <div className="card mb-6">
          <h2 className="card-title mb-4">{editing ? 'Editar webhook' : 'Nuevo webhook'}</h2>
          <form onSubmit={handleSubmit}>
            {!editing && (
              <div className="form-group">
                <label htmlFor="wh-cliente" className="label font-medium block mb-1">Cliente</label>
                <select
                  id="wh-cliente"
                  name="clienteId"
                  className="input w-full"
                  value={form.clienteId}
                  onChange={handleChange}
                  required
                >
                  <option value="">Seleccionar cliente</option>
                  {clientes.map((c) => (
                    <option key={c.id} value={c.id}>{c.nombre}</option>
                  ))}
                </select>
              </div>
            )}

            <div className="form-group">
              <label htmlFor="wh-url" className="label font-medium block mb-1">URL del webhook</label>
              <input
                id="wh-url"
                name="url"
                type="url"
                className="input w-full"
                value={form.url}
                onChange={handleChange}
                placeholder="https://cliente.com/api/webhook"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="wh-secret" className="label font-medium block mb-1">Secret token</label>
              <input
                id="wh-secret"
                name="secretToken"
                type="password"
                className="input w-full"
                value={form.secretToken}
                onChange={handleChange}
                placeholder={editing ? 'Déjalo vacío para no cambiarlo' : 'Token secreto para firmar las peticiones'}
              />
            </div>

            <div className="form-group">
              <label className="label font-medium block mb-1">
                <input
                  type="checkbox"
                  name="activo"
                  checked={form.activo}
                  onChange={handleChange}
                />
                {' '}Activo
              </label>
            </div>

            <div className="form-actions mt-4">
              <button type="submit" className="btn btn-luxury" disabled={saving}>
                {saving ? 'Guardando…' : (editing ? 'Guardar cambios' : 'Crear webhook')}
              </button>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => { setShowForm(false); setEditing(null) }}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}

      <section className="table-section">
        <div className="table-header">
          <h2>Webhooks</h2>
          <span className="table-count">{webhooks.length} webhook{webhooks.length !== 1 ? 's' : ''}</span>
        </div>

        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
            <button className="btn btn-outline ms-3" onClick={() => cargar(filtroCliente ? Number(filtroCliente) : undefined)}>
              Recargar datos
            </button>
          </div>
        )}

        {loading ? (
          <EmptyState message="Cargando webhooks…" />
        ) : webhooks.length === 0 ? (
          <EmptyState message="No hay webhooks configurados" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>URL</th>
                <th>Estado</th>
                <th>Creado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {webhooks.map((w) => (
                <Fragment key={w.id}>
                  <tr>
                    <td>{nombreCliente(w.clienteId)}</td>
                    <td className="cell-mensaje">{w.url}</td>
                    <td>
                      <span className={w.activo ? 'lote-badge lote-badge--success' : 'lote-badge lote-badge--warning'}>
                        {w.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="cell-date">
                      {w.fechaCreacion ? parseLocalDateTime(w.fechaCreacion).toLocaleString('es-ES') : '-'}
                    </td>
                    <td>
                      <div className="acciones-fila">
                        <button className="btn-nav-link" onClick={() => alternarLogs(w.id)}>
                          {logsAbiertos[w.id] ? 'Ocultar logs' : 'Ver logs'}
                        </button>
                        <button className="btn-nav-link" onClick={() => abrirEdicion(w)}>Editar</button>
                        <button className="btn-nav-link" onClick={() => eliminar(w.id)}>Eliminar</button>
                      </div>
                    </td>
                  </tr>
                  {logsAbiertos[w.id] && (
                    <tr>
                      <td colSpan={5}>
                        <div className="logs-webhook">
                          <h4>Historial de despachos</h4>
                          {cargandoLogs[w.id] ? (
                            <EmptyState message="Cargando logs…" />
                          ) : logsPorWebhook[w.id]?.length ? (
                            <table className="envios-table">
                              <thead>
                                <tr>
                                  <th>Envío</th>
                                  <th>Resultado</th>
                                  <th>Estado HTTP</th>
                                  <th>Error</th>
                                  <th>Fecha</th>
                                </tr>
                              </thead>
                              <tbody>
                                {logsPorWebhook[w.id].map((l) => (
                                  <tr key={l.id}>
                                    <td>{l.envioId}</td>
                                    <td>
                                      <span className={l.exitoso ? 'lote-badge lote-badge--success' : 'lote-badge lote-badge--danger'}>
                                        {l.exitoso ? 'Exitoso' : 'Fallido'}
                                      </span>
                                    </td>
                                    <td>{l.responseStatus ?? '-'}</td>
                                    <td className="cell-mensaje">{l.errorMensaje || '-'}</td>
                                    <td className="cell-date">
                                      {l.fechaCreacion ? parseLocalDateTime(l.fechaCreacion).toLocaleString('es-ES') : '-'}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          ) : (
                            <EmptyState message="Sin registros de despacho todavía" />
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  )
}
