export default function DateRangeFilter({ fechaDesde, fechaHasta, onChangeDesde, onChangeHasta }) {
  return (
    <div className="flex items-center gap-2">
      <input
        type="date"
        className="rounded-md border border-grafito-600 bg-grafito-900 px-2 py-2 text-sm text-grafito-100 outline-none transition-colors focus:border-[#d4762a]"
        value={fechaDesde}
        onChange={(e) => onChangeDesde(e.target.value)}
        title="Fecha desde"
        placeholder="Desde"
      />
      <span className="text-grafito-400">—</span>
      <input
        type="date"
        className="rounded-md border border-grafito-600 bg-grafito-900 px-2 py-2 text-sm text-grafito-100 outline-none transition-colors focus:border-[#d4762a]"
        value={fechaHasta}
        onChange={(e) => onChangeHasta(e.target.value)}
        title="Fecha hasta"
        placeholder="Hasta"
      />
    </div>
  );
}
