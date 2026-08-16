import SearchBar from './SearchBar';
import MultiStatusFilter from './MultiStatusFilter';
import DateRangeFilter from './DateRangeFilter';
import ActiveFilters from './ActiveFilters';

export default function EnvioFilters({
  query,
  estados,
  fechaDesde,
  fechaHasta,
  onQueryChange,
  onEstadosChange,
  onFechaDesdeChange,
  onFechaHastaChange,
  onRemoveEstado,
  onClearQuery,
  onClearFecha,
  onClearAll,
}) {
  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <div className="flex-1">
          <SearchBar value={query} onChange={onQueryChange} placeholder="Buscar por código, cliente o destinatario..." />
        </div>
        <DateRangeFilter
          fechaDesde={fechaDesde}
          fechaHasta={fechaHasta}
          onChangeDesde={onFechaDesdeChange}
          onChangeHasta={onFechaHastaChange}
        />
      </div>
      <MultiStatusFilter selected={estados} onChange={onEstadosChange} />
      <ActiveFilters
        estados={estados}
        query={query}
        fechaDesde={fechaDesde}
        fechaHasta={fechaHasta}
        onRemoveEstado={onRemoveEstado}
        onClearQuery={onClearQuery}
        onClearFecha={onClearFecha}
        onClearAll={onClearAll}
      />
    </div>
  );
}
