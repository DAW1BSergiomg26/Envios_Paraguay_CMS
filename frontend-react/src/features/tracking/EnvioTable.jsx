import TableSkeleton from '../../components/Skeleton'
import EmptyState from '../../components/EmptyState'
import Pagination from './Pagination'
import StatusBadge from './StatusBadge'

function formatFecha(fecha) {
  return new Date(fecha).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
}

export default function EnvioTable({
  envios = [],
  loading = false,
  page = 0,
  totalPages = 0,
  totalElements = 0,
  pageSize = 10,
  hasFilters = false,
  onPageChange,
  onPageSizeChange,
  onEdit,
  onDelete,
  onRowClick,
}) {
  return (
    <section className="rounded-xl border border-grafito-700 bg-grafito-900/50">
      <div className="flex items-center justify-between border-b border-grafito-700 px-5 py-4">
        <h2 className="text-lg font-semibold text-grafito-100">Envíos</h2>
        <span className="text-sm text-grafito-400">
          {totalElements} registro{totalElements !== 1 ? 's' : ''}
        </span>
      </div>

      {loading ? (
        <div data-testid="table-skeleton" className="p-5">
          <TableSkeleton rows={5} columns={7} />
        </div>
      ) : envios.length === 0 ? (
        <div className="p-8">
          <EmptyState
            message={hasFilters ? 'No se encontraron envíos con esos filtros' : 'No hay envíos registrados'}
          />
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-max text-sm">
            <thead>
              <tr className="border-b border-grafito-700 text-left text-xs uppercase tracking-wide text-grafito-400">
                <th className="px-5 py-3 font-medium">Código</th>
                <th className="px-5 py-3 font-medium">Estado</th>
                <th className="px-5 py-3 font-medium">Destinatario</th>
                <th className="px-5 py-3 font-medium">Origen</th>
                <th className="px-5 py-3 font-medium">Destino</th>
                <th className="px-5 py-3 font-medium">Fecha</th>
                <th className="px-5 py-3 text-right font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-grafito-800">
              {envios.map((envio) => (
                <tr
                  key={envio.codigoUnico}
                  className="cursor-pointer transition-colors hover:bg-grafito-800/50"
                  onClick={() => onRowClick?.(envio.codigoUnico)}
                >
                  <td className="px-5 py-3 font-medium text-[#d4762a]">{envio.codigoUnico}</td>
                  <td className="px-5 py-3">
                    <StatusBadge estado={envio.estado} />
                  </td>
                  <td className="px-5 py-3 text-grafito-200">{envio.destinatario}</td>
                  <td className="px-5 py-3 text-grafito-300">{envio.origen}</td>
                  <td className="px-5 py-3 text-grafito-300">{envio.destino}</td>
                  <td className="px-5 py-3 text-grafito-400">
                    {formatFecha(envio.ultimaActualizacion)}
                  </td>
                  <td className="px-5 py-3 text-right">
                    <button
                      type="button"
                      className="mr-2 rounded-md border border-grafito-600 px-2.5 py-1 text-xs font-medium text-grafito-200 transition-colors hover:bg-grafito-700"
                      onClick={(ev) => { ev.stopPropagation(); onEdit?.(envio.codigoUnico); }}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="rounded-md border border-red-800/60 px-2.5 py-1 text-xs font-medium text-red-400 transition-colors hover:bg-red-900/30"
                      onClick={(ev) => { ev.stopPropagation(); onDelete?.(envio); }}
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="border-t border-grafito-700 px-5 py-4">
            <Pagination
              page={page}
              totalPages={totalPages}
              totalElements={totalElements}
              pageSize={pageSize}
              onChange={onPageChange}
              onPageSizeChange={onPageSizeChange}
            />
          </div>
        </div>
      )}
    </section>
  );
}
