export default function AnalyticsKPIs({ envios = [], loading }) {
  if (loading) {
    return (
      <div className="kpi-grid">
        {Array.from({ length: 5 }, (_, i) => (
          <div key={i} className="analytics-card">
            <div className="skeleton skeleton--sm" style={{ marginBottom: '8px' }} />
            <div className="skeleton skeleton--value" />
            <div className="skeleton skeleton--sm" style={{ marginTop: '6px' }} />
          </div>
        ))}
      </div>
    );
  }

  const total = envios.length;
  const entregados = envios.filter(e => e.estado === 'ENTREGADO').length;
  const enTransito = envios.filter(e => e.estado === 'EN_TRANSITO').length;
  const enAduana = envios.filter(e => e.estado?.includes('ADUANA')).length;
  const pendientes = envios.filter(e => e.estado === 'RECIBIDO').length;
  const tasa = total > 0 ? ((entregados / total) * 100).toFixed(1) : '0.0';

  const kpis = [
    {
      label: 'Tasa de entrega',
      value: `${tasa}%`,
      sub: `${entregados} de ${total} envíos`,
      color: '#10b981',
      icon: '🎯'
    },
    {
      label: 'Envíos activos',
      value: total - entregados,
      sub: enTransito + enAduana + pendientes + ' en proceso',
      color: '#3b82f6',
      icon: '📦'
    },
    {
      label: 'En aduana',
      value: enAduana,
      sub: `${total > 0 ? ((enAduana / total) * 100).toFixed(0) : 0}% del total`,
      color: '#f59e0b',
      icon: '🛃'
    },
    {
      label: 'En tránsito',
      value: enTransito,
      sub: `${total > 0 ? ((enTransito / total) * 100).toFixed(0) : 0}% del total`,
      color: '#8b5cf6',
      icon: '🚢'
    },
    {
      label: 'Pendientes',
      value: pendientes,
      sub: `${total > 0 ? ((pendientes / total) * 100).toFixed(0) : 0}% del total`,
      color: '#6b7280',
      icon: '📋'
    }
  ];

  return (
    <div className="kpi-grid">
      {kpis.map((kpi, i) => (
        <div key={i} className="kpi-card">
          <div className="kpi-header">
            <span className="kpi-icon" style={{ background: kpi.color + '15' }}>{kpi.icon}</span>
            <span className="kpi-label">{kpi.label}</span>
          </div>
          <span className="kpi-value" style={{ color: kpi.color }}>{kpi.value}</span>
          <span className="kpi-sub">{kpi.sub}</span>
        </div>
      ))}
    </div>
  );
}
