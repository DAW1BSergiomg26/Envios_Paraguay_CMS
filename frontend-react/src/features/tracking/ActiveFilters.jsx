import { ESTADO_LABELS } from './trackingConstants';

export default function ActiveFilters({
  estados, query, fechaDesde, fechaHasta,
  onRemoveEstado, onClearQuery, onClearFecha, onClearAll,
}) {
  const chips = [];

  estados.forEach((e) => {
    chips.push({ key: `estado-${e}`, label: ESTADO_LABELS[e] || e, onRemove: () => onRemoveEstado(e) });
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
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex flex-wrap gap-2">
        {chips.map((chip) => (
          <span
            key={chip.key}
            className="inline-flex items-center gap-1.5 rounded-full border border-grafito-600 bg-grafito-900 px-3 py-1 text-xs text-grafito-200"
          >
            {chip.label}
            <button
              type="button"
              className="text-grafito-300 transition-colors hover:text-white"
              onClick={chip.onRemove}
            >
              &times;
            </button>
          </span>
        ))}
      </div>
      <button
        type="button"
        className="text-sm text-[#d4762a] transition-colors hover:underline"
        onClick={onClearAll}
      >
        Limpiar filtros
      </button>
    </div>
  );
}
