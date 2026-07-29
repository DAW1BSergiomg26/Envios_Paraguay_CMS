const COLORS = {
  RECIBIDO: '#6b7280',
  EN_ADUANA_ORIGEN: '#f59e0b',
  EN_TRANSITO: '#3b82f6',
  EN_ADUANA_DESTINO: '#f59e0b',
  EN_REPARTO: '#8b5cf6',
  ENTREGADO: '#10b981'
};

export default function StatusBadge({ estado }) {
  const color = COLORS[estado] || '#6b7280';
  return (
    <span className="status-badge" style={{ backgroundColor: color + '1a', color, borderColor: color + '40' }}>
      <span className="status-badge-dot" style={{ backgroundColor: color }} />
      {estado?.replace(/_/g, ' ') || 'N/A'}
    </span>
  );
}
