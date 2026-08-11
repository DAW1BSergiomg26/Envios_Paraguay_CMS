import { useState, useEffect, useCallback, Fragment } from 'react'
import {
  listarNotificaciones,
  reintentarNotificacion,
} from '../services/api'
import { parseLocalDateTime } from '../services/dateUtils'
import { useToast } from '../context/NotificationContext'
import EmptyState from '../components/EmptyState'

const ESTADOS = ['ENVIADO', 'FALLIDO', 'OMITIDO_SIN_DESTINATARIO']

const ETIQUETA_ESTADO = {
  ENVIADO: 'Enviado',
  FALLIDO: 'Fallido',
  OMITIDO_SIN_DESTINATARIO: 'Omitido sin destinatario',
}

export default function NotificacionesPage() {
  const { showSuccess, showError } = useToast()
  const [notificaciones, setNotificaciones] = useState([])
  const [filtroEstado, setFiltroEstado] = useState('')
  const [loading, setLoading] = useState(true)
  const [expandida, setExpandida] = useState(null)
  const [reintentando, setReintentando] = useState(null)

  const cargar = useCallback(async (estado) => {
    try {
      setLoading(true)
      const res = await listarNotificaciones(estado || undefined)
      setNotificaciones(res.data || [])
    } catch (err) {
      showError(err.message || 'Error al cargar las notificaciones')
    } finally {
      setLoading(false)
    }
  }, [showError])

  useEffect(() => {
    cargar(filtroEstado)
  }, [filtroEstado, cargar])

  const reintentar = async (id) => {
    setReintentando(id)
    try {
      await reintentarNotificacion(id)
      showSuccess('Notificación reenviada')
      await cargar(filtroEstado)
    } catch (err) {
      showError(err.message || 'Error al reintentar la notificación')
      await cargar(filtroEstado)
    } finally {
      setReintentando(null)
    }
  }

  const badgeClase = (estado) => {
    if (estado === 'ENVIADO') return 'lote-badge lote-badge--success'
    if (estado === 'FALLIDO') return 'lote-badge lote-badge--danger'
    return 'lote-badge lote-badge--warning'
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Notificaciones</h1>
          <p className="dashboard-subtitle">
            Historial de correos de notificación de estado hacia los clientes.
          </p>
        </div>
      </header>

      <div className="import-form-row">
        <div className="form-group mb-0">
          <label htmlFor="filtroEstado" className="label font-medium block mb-1">Filtrar por estado</label>
          <select
            id="filtroEstado"
            className="import-select"
            value={filtroEstado}
            onChange={(e) => setFiltroEstado(e.target.value)}
          >
            <option value="">Todos los estados</option>
            {ESTADOS.map((estado) => (
              <option key={estado} value={estado}>{ETIQUETA_ESTADO[estado]}</option>
            ))}
          </select>
        </div>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Historial</h2>
          <span className="table-count">{notificaciones.length} notificación{notificaciones.length !== 1 ? 'es' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando notificaciones…" />
        ) : notificaciones.length === 0 ? (
          <EmptyState message="No hay notificaciones" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Asunto</th>
                <th>Envío</th>
                <th>Estado</th>
                <th>Fecha</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {notificaciones.map((n) => (
                <Fragment key={n.id}>
                  <tr
                    className="fila-expandible"
                    onClick={() => setExpandida(expandida === n.id ? null : n.id)}
                  >
                    <td className="cell-mensaje">{n.asunto}</td>
                    <td>{n.envioId}</td>
                    <td>
                      <span className={badgeClase(n.estado)}>
                        {ETIQUETA_ESTADO[n.estado] || n.estado}
                      </span>
                    </td>
                    <td className="cell-date">
                      {n.fechaCreacion ? parseLocalDateTime(n.fechaCreacion).toLocaleString('es-ES') : '-'}
                    </td>
                    <td onClick={(e) => e.stopPropagation()}>
                      {n.estado === 'FALLIDO' && (
                        <button
                          className="btn-nav-link"
                          onClick={() => reintentar(n.id)}
                          disabled={reintentando === n.id}
                        >
                          {reintentando === n.id ? 'Reenviando…' : 'Reintentar'}
                        </button>
                      )}
                    </td>
                  </tr>
                  {expandida === n.id && (
                    <tr>
                      <td colSpan={5}>
                        <div className="notificacion-detalle">
                          <p><strong>Destinatario:</strong> {n.destinatario || '-'}</p>
                          <p><strong>Mensaje:</strong> {n.mensaje}</p>
                          {n.errorMensaje && <p><strong>Error:</strong> {n.errorMensaje}</p>}
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
