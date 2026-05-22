import { parseLocalDateTime } from '../services/dateUtils';

const ICONS = {
  RECIBIDO: '📋',
  EN_ADUANA_ORIGEN: '🛃',
  EN_TRANSITO: '🚢',
  EN_ADUANA_DESTINO: '🛃',
  EN_REPARTO: '🚚',
  ENTREGADO: '✅'
};

const COLORS = {
  RECIBIDO: '#6b7280',
  EN_ADUANA_ORIGEN: '#f59e0b',
  EN_TRANSITO: '#3b82f6',
  EN_ADUANA_DESTINO: '#f59e0b',
  EN_REPARTO: '#8b5cf6',
  ENTREGADO: '#10b981'
};

export default function Timeline({ eventos = [] }) {
  if (!eventos.length) return null;

  const sorted = [...eventos].sort((a, b) => {
    const da = parseLocalDateTime(a.fecha);
    const db = parseLocalDateTime(b.fecha);
    return da.getTime() - db.getTime();
  });

  return (
    <div className="timeline">
      {sorted.map((ev, i) => {
        const color = COLORS[ev.tipo] || '#6b7280';
        const icon = ICONS[ev.tipo] || '📌';
        const isLast = i === sorted.length - 1;
        return (
          <div key={i} className="timeline-item">
            <div className="timeline-line" style={{ background: isLast ? 'transparent' : '#2a2d3a' }} />
            <div className="timeline-dot" style={{ borderColor: color, background: color + '20' }}>
              <span className="timeline-icon">{icon}</span>
            </div>
            <div className="timeline-content">
              <span className="timeline-date" style={{ color }}>
                {parseLocalDateTime(ev.fecha).toLocaleDateString('es-ES', {
                  day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
                })}
              </span>
              <span className="timeline-tag" style={{ background: color + '1a', color, borderColor: color + '40' }}>
                {ev.tipo?.replace(/_/g, ' ') || 'Evento'}
              </span>
              <p className="timeline-desc">{ev.descripcion}</p>
            </div>
          </div>
        );
      })}
    </div>
  );
}
