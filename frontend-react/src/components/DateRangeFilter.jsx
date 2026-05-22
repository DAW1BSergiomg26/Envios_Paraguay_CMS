export default function DateRangeFilter({ fechaDesde, fechaHasta, onChangeDesde, onChangeHasta }) {
  return (
    <div className="date-range-filter">
      <input
        type="date"
        className="date-input"
        value={fechaDesde}
        onChange={e => onChangeDesde(e.target.value)}
        title="Fecha desde"
        placeholder="Desde"
      />
      <span className="date-range-sep">—</span>
      <input
        type="date"
        className="date-input"
        value={fechaHasta}
        onChange={e => onChangeHasta(e.target.value)}
        title="Fecha hasta"
        placeholder="Hasta"
      />
    </div>
  );
}
