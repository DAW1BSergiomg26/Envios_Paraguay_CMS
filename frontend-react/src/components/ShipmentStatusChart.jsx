import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const COLORS = {
  RECIBIDO: '#6b7280',
  EN_ADUANA_ORIGEN: '#f59e0b',
  EN_TRANSITO: '#3b82f6',
  EN_ADUANA_DESTINO: '#f59e0b',
  EN_REPARTO: '#8b5cf6',
  ENTREGADO: '#10b981'
};

const LABELS = {
  RECIBIDO: 'Pendiente',
  EN_ADUANA_ORIGEN: 'En aduana origen',
  EN_TRANSITO: 'En tránsito',
  EN_ADUANA_DESTINO: 'En aduana destino',
  EN_REPARTO: 'En reparto',
  ENTREGADO: 'Entregado'
};

function aggregateByStatus(envios) {
  const counts = {};
  for (const e of envios) {
    counts[e.estado] = (counts[e.estado] || 0) + 1;
  }
  return Object.entries(counts).map(([estado, value]) => ({
    name: LABELS[estado] || estado,
    value,
    color: COLORS[estado] || '#6b7280',
    estado
  }));
}

const RADIAN = Math.PI / 180;

function renderLabel({ cx, cy, midAngle, innerRadius, outerRadius, percent, value }) {
  if (value === 0) return null;
  const radius = innerRadius + (outerRadius - innerRadius) * 1.35;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="#9ca3af" textAnchor={x > cx ? 'start' : 'end'} dominantBaseline="central" fontSize={11}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
}

function CustomTooltip({ active, payload }) {
  if (!active || !payload?.length) return null;
  const d = payload[0].payload;
  return (
    <div className="chart-tooltip">
      <span className="chart-tooltip-label">{d.name}</span>
      <span className="chart-tooltip-value">{d.value} envío{d.value !== 1 ? 's' : ''}</span>
    </div>
  );
}

function ChartSkeleton() {
  return (
    <div className="chart-skeleton">
      <div className="skeleton" style={{ width: '160px', height: '160px', borderRadius: '50%', margin: '0 auto' }} />
      <div className="skeleton skeleton--md" style={{ margin: '0.75rem auto 0' }} />
    </div>
  );
}

export default function ShipmentStatusChart({ envios = [], loading }) {
  if (loading || !envios.length) {
    return (
      <div className="analytics-card">
        <h3 className="analytics-card-title">Estado de envíos</h3>
        <ChartSkeleton />
      </div>
    );
  }

  const data = aggregateByStatus(envios);
  const total = data.reduce((s, d) => s + d.value, 0);

  if (total === 0) {
    return (
      <div className="analytics-card">
        <h3 className="analytics-card-title">Estado de envíos</h3>
        <div className="chart-empty">Sin datos para mostrar</div>
      </div>
    );
  }

  return (
    <div className="analytics-card">
      <h3 className="analytics-card-title">Estado de envíos</h3>
      <div className="donut-wrapper">
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={55}
              outerRadius={80}
              paddingAngle={3}
              dataKey="value"
              stroke="none"
            >
              {data.map((entry, i) => (
                <Cell key={i} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
          </PieChart>
        </ResponsiveContainer>
        <div className="donut-center">
          <span className="donut-center-value">{total}</span>
          <span className="donut-center-label">Total</span>
        </div>
      </div>
      <div className="donut-legend">
        {data.map((d, i) => (
          <div key={i} className="donut-legend-item">
            <span className="donut-legend-dot" style={{ background: d.color }} />
            <span className="donut-legend-name">{d.name}</span>
            <span className="donut-legend-value">{d.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
