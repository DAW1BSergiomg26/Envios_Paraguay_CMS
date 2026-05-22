const STATUS_LABELS = {
  RECIBIDO: 'Recibido',
  EN_ADUANA_ORIGEN: 'Aduana Origen',
  EN_TRANSITO: 'En Tránsito',
  EN_ADUANA_DESTINO: 'Aduana Destino',
  EN_REPARTO: 'En Reparto',
  ENTREGADO: 'Entregado'
};

export default function ActiveFilters({
  estados, query, fechaDesde, fechaHasta,
  onRemoveEstado, onClearQuery, onClearFecha, onClearAll
}) {
  const chips = [];

  estados.forEach(e => {
    chips.push({ key: `estado-${e}`, label: STATUS_LABELS[e] || e, onRemove: () => onRemoveEstado(e) });
  });

  if (query) {
    chips.push({ key: 'query', label: `"${query}"`, onRemove: onClearQuery });
  }

  if (fechaDesde || fechaHasta) {
    let label = 'Fechas: ';
    if (fechaDesde) label += `desde ${fechaDesde}`;
    if (fechaDesde && fechaHasta) label += ' ';
    if (fechaHasta) label += `hasta ${fechaHasta}`;
    chips.push({ key: 'fecha', label, onRemove: onClearFecha });
  }

  if (chips.length === 0) return null;

  return (
    <div className="active-filters">
      <div className="active-filters__chips">
        {chips.map(chip => (
          <span key={chip.key} className="filter-chip">
            {chip.label}
            <button className="filter-chip__remove" onClick={chip.onRemove} type="button">&times;</button>
          </span>
        ))}
      </div>
      <button className="active-filters__clear" onClick={onClearAll} type="button">
        Limpiar filtros
      </button>
    </div>
  );
}
