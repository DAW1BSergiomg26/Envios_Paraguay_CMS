import { useState } from 'react';
import { putAdminEnvioEstado } from '../services/api';
import { useToast } from '../context/NotificationContext';

const ESTADOS = [
  { value: 'RECIBIDO', label: 'Recibido', color: '#6b7280' },
  { value: 'EN_ADUANA_ORIGEN', label: 'En aduana origen', color: '#f59e0b' },
  { value: 'EN_TRANSITO', label: 'En tránsito', color: '#3b82f6' },
  { value: 'EN_ADUANA_DESTINO', label: 'En aduana destino', color: '#f59e0b' },
  { value: 'EN_REPARTO', label: 'En reparto', color: '#8b5cf6' },
  { value: 'ENTREGADO', label: 'Entregado', color: '#10b981' }
];

export default function UpdateEstadoPanel({ codigo, estadoActual, onUpdated }) {
  const [selected, setSelected] = useState('');
  const [updating, setUpdating] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const { showSuccess, showError: showErrToast } = useToast();

  const handleUpdate = async () => {
    if (!selected || selected === estadoActual) return;
    setUpdating(true);
    setFeedback(null);
    try {
      const res = await putAdminEnvioEstado(codigo, selected);
      setFeedback({ type: 'success', message: 'Estado actualizado correctamente' });
      showSuccess('Estado actualizado correctamente');
      setSelected('');
      if (onUpdated) onUpdated(res.data);
    } catch (err) {
      const msg = err.response?.data?.error || err.message || 'Error al actualizar';
      setFeedback({ type: 'error', message: msg });
      showErrToast(msg);
    } finally {
      setUpdating(false);
    }
  };

  const available = ESTADOS.filter(e => e.value !== estadoActual);

  return (
    <section className="update-estado-section">
      <h2 className="section-title">Acción operativa</h2>
      <div className="update-estado-card">
        <p className="update-estado-current">
          Estado actual: <strong>{ESTADOS.find(e => e.value === estadoActual)?.label || estadoActual}</strong>
        </p>
        <div className="update-estado-controls">
          <select
            className="update-estado-select"
            value={selected}
            onChange={e => { setSelected(e.target.value); setFeedback(null); }}
            disabled={updating}
          >
            <option value="">Seleccionar nuevo estado…</option>
            {available.map(e => (
              <option key={e.value} value={e.value}>{e.label}</option>
            ))}
          </select>
          <button
            className="update-estado-btn"
            onClick={handleUpdate}
            disabled={!selected || selected === estadoActual || updating}
          >
            {updating ? (
              <>
                <span className="update-spinner" />
                Actualizando…
              </>
            ) : (
              'Actualizar estado'
            )}
          </button>
        </div>
        {feedback && (
          <div className={`update-feedback update-feedback--${feedback.type}`}>
            {feedback.type === 'success' ? '✅' : '⚠️'} {feedback.message}
          </div>
        )}
      </div>
    </section>
  );
}
