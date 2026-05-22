import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, Cell } from 'recharts';

function groupByDate(envios) {
  const counts = {};
  for (const e of envios) {
    if (!e.ultimaActualizacion) continue;
    let d;
    if (Array.isArray(e.ultimaActualizacion)) {
      const [y, m, day] = e.ultimaActualizacion;
      d = `${y}-${String(m).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    } else {
      d = e.ultimaActualizacion.split('T')[0];
    }
    counts[d] = (counts[d] || 0) + 1;
  }
  return Object.entries(counts)
    .sort(([a], [b]) => a.localeCompare(b))
    .slice(-14)
    .map(([date, count]) => {
      const parts = date.split('-');
      return {
        date,
        label: `${parts[2]}/${parts[1]}`,
        actividades: count
      };
    });
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      <span className="chart-tooltip-label">{label}</span>
      <span className="chart-tooltip-value">{payload[0].value} actividad{payload[0].value !== 1 ? 'es' : ''}</span>
    </div>
  );
}

function ChartSkeleton() {
  return (
    <div className="chart-skeleton">
      <div className="skeleton" style={{ width: '100%', height: '160px', borderRadius: '8px' }} />
      <div className="skeleton skeleton--md" style={{ margin: '0.75rem auto 0' }} />
    </div>
  );
}

export default function ActivityChart({ envios = [], loading }) {
  if (loading || !envios.length) {
    return (
      <div className="analytics-card">
        <h3 className="analytics-card-title">Actividad reciente</h3>
        <ChartSkeleton />
      </div>
    );
  }

  const data = groupByDate(envios);

  if (data.length === 0) {
    return (
      <div className="analytics-card">
        <h3 className="analytics-card-title">Actividad reciente</h3>
        <div className="chart-empty">Sin datos para mostrar</div>
      </div>
    );
  }

  return (
    <div className="analytics-card">
      <h3 className="analytics-card-title">Actividad reciente</h3>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={data} margin={{ top: 5, right: 5, left: -15, bottom: 0 }}>
          <XAxis
            dataKey="label"
            tick={{ fill: '#6b7280', fontSize: 10 }}
            axisLine={{ stroke: '#2a2d3a' }}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fill: '#6b7280', fontSize: 10 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip content={<CustomTooltip />} cursor={{ fill: '#2a2d3a' }} />
          <Bar dataKey="actividades" radius={[4, 4, 0, 0]} maxBarSize={32}>
            {data.map((_, i) => (
              <Cell key={i} fill="#3b82f6" />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
