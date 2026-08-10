import { useState, useEffect, useCallback } from 'react';
import {
  getAdminReservas,
  patchAdminReservaEstado,
  putAdminReserva,
  deleteAdminReserva,
} from '../services/api';
import { parseLocalDateTime } from '../services/dateUtils';
import { useToast } from '../context/NotificationContext';
import EmptyState from '../components/EmptyState';

const ESTADOS = [
  { value: '', label: 'Todos los estados' },
  { value: 'pendiente', label: 'Pendiente' },
  { value: 'aprobada', label: 'Aprobada' },
  { value: 'confirmada', label: 'Confirmada' },
  { value: 'cancelada', label: 'Cancelada' },
];

const ESTADO_BADGE = {
  pendiente: 'lote-badge lote-badge--warning',
  aprobada: 'lote-badge lote-badge--info',
  confirmada: 'lote-badge lote-badge--success',
  cancelada: 'lote-badge lote-badge--danger',
};

const ESTADO_LABEL = {
  pendiente: 'Pendiente',
  aprobada: 'Aprobada',
  confirmada: 'Confirmada',
  cancelada: 'Cancelada',
};

const ACTIONS_POR_ESTADO = {
  pendiente: ['aprobar'],
  aprobada: ['confirmar'],
  confirmada: ['cancelar'],
  cancelada: [],
};

export default function ReservasPage() {
  const { showSuccess, showError } = useToast();
  const [reservas, setReservas] = useState([]);
  const [estado, setEstado] = useState('');
  const [loading, setLoading] = useState(true);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(null);

  const cargar = useCallback(async (estadoFiltro) => {
    try {
      const res = await getAdminReservas(estadoFiltro || undefined);
      setReservas(res.data || []);
    } catch (err) {
      showError(err.message || 'Error al cargar las reservas');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    setLoading(true);
    cargar(estado);
  }, [estado, cargar]);

  const cambiarEstado = async (id, nuevoEstado) => {
    try {
      await patchAdminReservaEstado(id, nuevoEstado);
      showSuccess('Estado actualizado');
      cargar(estado || undefined);
    } catch (err) {
      showError(err.message || 'Error al actualizar el estado');
    }
  };

  const abrirEdicion = (r) => {
    setEditando(r);
    setForm({
      nombreCliente: r.nombreCliente || '',
      email: r.email || '',
      telefono: r.telefono || '',
      fechaEntrada: r.fechaEntrada || '',
      fechaSalida: r.fechaSalida || '',
      numeroHuespedes: r.numeroHuespedes || 1,
      comentarios: r.comentarios || '',
    });
  };

  const guardarEdicion = async () => {
    try {
      await putAdminReserva(editando.id, form);
      showSuccess('Reserva actualizada');
      setEditando(null);
      cargar(estado || undefined);
    } catch (err) {
      showError(err.message || 'Error al guardar la reserva');
    }
  };

  const eliminar = async (id) => {
    if (!window.confirm('¿Seguro que quieres eliminar esta reserva?')) return;
    try {
      await deleteAdminReserva(id);
      showSuccess('Reserva eliminada');
      cargar(estado || undefined);
    } catch (err) {
      showError(err.message || 'Error al eliminar la reserva');
    }
  };

  const renderAcciones = (r) => {
    const acciones = ACTIONS_POR_ESTADO[r.estado] || [];
    return (
      <div className="acciones-fila">
        {acciones.includes('aprobar') && (
          <button className="btn-nav-link" onClick={() => cambiarEstado(r.id, 'aprobada')}>Aprobar</button>
        )}
        {acciones.includes('confirmar') && (
          <button className="btn-nav-link" onClick={() => cambiarEstado(r.id, 'confirmada')}>Confirmar</button>
        )}
        {acciones.includes('cancelar') && (
          <button className="btn-nav-link" onClick={() => cambiarEstado(r.id, 'cancelada')}>Cancelar</button>
        )}
        <button className="btn-nav-link" onClick={() => abrirEdicion(r)}>Editar</button>
        <button className="btn-nav-link" onClick={() => eliminar(r.id)}>Eliminar</button>
      </div>
    );
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Reservas</h1>
          <p className="dashboard-subtitle">Gestiona las reservas de envíos recibidas.</p>
        </div>
      </header>

      <div className="import-form-row">
        <select
          id="estadoFiltro"
          className="import-select"
          value={estado}
          onChange={e => setEstado(e.target.value)}
          aria-label="Filtrar por estado"
        >
          {ESTADOS.map(e => (
            <option key={e.value} value={e.value}>{e.label}</option>
          ))}
        </select>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Reservas</h2>
          <span className="table-count">{reservas.length} reserva{reservas.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando reservas…" />
        ) : reservas.length === 0 ? (
          <EmptyState message="No hay reservas todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Cliente</th><th>Fechas</th><th>Huéspedes</th>
                <th>Estado</th><th>Creada</th><th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {reservas.map(r => (
                <tr key={r.id}>
                  <td>
                    <div className="cell-nombre">{r.nombreCliente}</div>
                    <div className="cell-sub">{r.email}</div>
                  </td>
                  <td className="cell-date">{r.fechaEntrada} → {r.fechaSalida}</td>
                  <td>{r.numeroHuespedes}</td>
                  <td><span className={ESTADO_BADGE[r.estado] || 'lote-badge'}>{ESTADO_LABEL[r.estado] || r.estado}</span></td>
                  <td className="cell-date">
                    {r.createdAt
                      ? parseLocalDateTime(r.createdAt).toLocaleString('es-ES', {
                          day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                        })
                      : '-'}
                  </td>
                  <td>{renderAcciones(r)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {editando && (
        <div className="import-modal" role="dialog" aria-modal="true" aria-label="Editar reserva">
          <div className="import-modal-content">
            <div className="import-modal-header">
              <h3>Editar reserva</h3>
              <button type="button" className="import-modal-close" aria-label="Cerrar" onClick={() => setEditando(null)}>×</button>
            </div>
            <div className="form-edicion">
              <label className="import-label" htmlFor="nombreCliente">Nombre</label>
              <input id="nombreCliente" className="import-input" value={form.nombreCliente}
                onChange={e => setForm({ ...form, nombreCliente: e.target.value })} />
              <label className="import-label" htmlFor="email">Email</label>
              <input id="email" className="import-input" value={form.email}
                onChange={e => setForm({ ...form, email: e.target.value })} />
              <label className="import-label" htmlFor="telefono">Teléfono</label>
              <input id="telefono" className="import-input" value={form.telefono}
                onChange={e => setForm({ ...form, telefono: e.target.value })} />
              <label className="import-label" htmlFor="fechaEntrada">Fecha entrada</label>
              <input id="fechaEntrada" className="import-input" type="date" value={form.fechaEntrada}
                onChange={e => setForm({ ...form, fechaEntrada: e.target.value })} />
              <label className="import-label" htmlFor="fechaSalida">Fecha salida</label>
              <input id="fechaSalida" className="import-input" type="date" value={form.fechaSalida}
                onChange={e => setForm({ ...form, fechaSalida: e.target.value })} />
              <label className="import-label" htmlFor="numeroHuespedes">Huéspedes</label>
              <input id="numeroHuespedes" className="import-input" type="number" min="1" value={form.numeroHuespedes}
                onChange={e => setForm({ ...form, numeroHuespedes: Number(e.target.value) })} />
              <label className="import-label" htmlFor="comentarios">Comentarios</label>
              <textarea id="comentarios" className="import-input" value={form.comentarios}
                onChange={e => setForm({ ...form, comentarios: e.target.value })} />
              <div className="import-form-row">
                <button className="btn-importar btn-importar--small" onClick={guardarEdicion}>Guardar</button>
                <button className="btn-nav-link" onClick={() => setEditando(null)}>Cancelar</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
