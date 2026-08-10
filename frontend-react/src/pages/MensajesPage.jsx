import { useState, useEffect, useCallback } from 'react';
import { getAdminMensajes, patchAdminMensajeLeido, deleteAdminMensaje } from '../services/api';
import { parseLocalDateTime } from '../services/dateUtils';
import { useToast } from '../context/NotificationContext';
import EmptyState from '../components/EmptyState';

const FILTROS = [
  { value: '', label: 'Todos' },
  { value: 'no_leido', label: 'No leídos' },
  { value: 'leido', label: 'Leídos' },
];

export default function MensajesPage() {
  const { showSuccess, showError } = useToast();
  const [mensajes, setMensajes] = useState([]);
  const [filtro, setFiltro] = useState('');
  const [loading, setLoading] = useState(true);

  const cargar = useCallback(async (filtroActual) => {
    try {
      let leido;
      if (filtroActual === 'no_leido') leido = false;
      if (filtroActual === 'leido') leido = true;
      const res = await getAdminMensajes(leido);
      setMensajes(res.data || []);
    } catch (err) {
      showError(err.message || 'Error al cargar los mensajes');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    setLoading(true);
    cargar(filtro);
  }, [filtro, cargar]);

  const marcarLeido = async (id) => {
    try {
      await patchAdminMensajeLeido(id, true);
      showSuccess('Mensaje marcado como leído');
      cargar(filtro);
    } catch (err) {
      showError(err.message || 'Error al marcar el mensaje');
    }
  };

  const eliminar = async (id) => {
    if (!window.confirm('¿Seguro que quieres eliminar este mensaje?')) return;
    try {
      await deleteAdminMensaje(id);
      showSuccess('Mensaje eliminado');
      cargar(filtro);
    } catch (err) {
      showError(err.message || 'Error al eliminar el mensaje');
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Mensajes de contacto</h1>
          <p className="dashboard-subtitle">Solicitudes e incidencias recibidas desde el formulario de contacto.</p>
        </div>
      </header>

      <div className="import-form-row">
        <select
          id="filtroLeido"
          className="import-select"
          value={filtro}
          onChange={e => setFiltro(e.target.value)}
          aria-label="Filtrar por estado de lectura"
        >
          {FILTROS.map(f => (
            <option key={f.value} value={f.value}>{f.label}</option>
          ))}
        </select>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Mensajes</h2>
          <span className="table-count">{mensajes.length} mensaje{mensajes.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando mensajes…" />
        ) : mensajes.length === 0 ? (
          <EmptyState message="No hay mensajes de contacto todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Nombre</th><th>Email</th><th>Mensaje</th>
                <th>Estado</th><th>Fecha</th><th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {mensajes.map(m => (
                <tr key={m.id}>
                  <td>
                    <div className="cell-nombre">{m.nombre}</div>
                    <div className="cell-sub">{m.telefono || '-'}</div>
                  </td>
                  <td>{m.email}</td>
                  <td className="cell-mensaje">{m.mensaje}</td>
                  <td>
                    <span className={m.leido ? 'lote-badge lote-badge--success' : 'lote-badge lote-badge--warning'}>
                      {m.leido ? 'Leído' : 'No leído'}
                    </span>
                  </td>
                  <td className="cell-date">
                    {m.fechaEnvio
                      ? parseLocalDateTime(m.fechaEnvio).toLocaleString('es-ES', {
                          day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                        })
                      : '-'}
                  </td>
                  <td>
                    <div className="acciones-fila">
                      {!m.leido && (
                        <button className="btn-nav-link" onClick={() => marcarLeido(m.id)}>Marcar leído</button>
                      )}
                      <button className="btn-nav-link" onClick={() => eliminar(m.id)}>Eliminar</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
