const STATUSES = [
  { value: 'RECIBIDO', label: 'Recibido' },
  { value: 'EN_ADUANA_ORIGEN', label: 'Aduana Origen' },
  { value: 'EN_TRANSITO', label: 'En Tránsito' },
  { value: 'EN_ADUANA_DESTINO', label: 'Aduana Destino' },
  { value: 'EN_REPARTO', label: 'En Reparto' },
  { value: 'ENTREGADO', label: 'Entregado' }
];

export default function MultiStatusFilter({ selected, onChange }) {
  const toggle = (value) => {
    const next = selected.includes(value)
      ? selected.filter(s => s !== value)
      : [...selected, value];
    onChange(next);
  };

  return (
    <div className="multi-status-filter">
      {STATUSES.map(s => {
        const active = selected.includes(s.value);
        return (
          <button
            key={s.value}
            className={`status-chip ${active ? 'status-chip--active' : ''}`}
            onClick={() => toggle(s.value)}
            type="button"
          >
            {s.label}
          </button>
        );
      })}
    </div>
  );
}
