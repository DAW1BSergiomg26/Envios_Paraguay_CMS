const STATUSES = [
  { value: '', label: 'Todos los estados' },
  { value: 'RECIBIDO', label: 'Recibido' },
  { value: 'EN_ADUANA_ORIGEN', label: 'Aduana Origen' },
  { value: 'EN_TRANSITO', label: 'En Tránsito' },
  { value: 'EN_ADUANA_DESTINO', label: 'Aduana Destino' },
  { value: 'EN_REPARTO', label: 'En Reparto' },
  { value: 'ENTREGADO', label: 'Entregado' }
];

export default function StatusFilter({ value, onChange }) {
  return (
    <select className="status-filter" value={value || ''} onChange={e => onChange(e.target.value)}>
      {STATUSES.map(s => (
        <option key={s.value} value={s.value}>{s.label}</option>
      ))}
    </select>
  );
}
