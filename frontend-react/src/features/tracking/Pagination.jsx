import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from './trackingConstants';

export default function Pagination({
  page,
  totalPages,
  totalElements,
  pageSize = DEFAULT_PAGE_SIZE,
  sizes = PAGE_SIZE_OPTIONS,
  onChange,
  onPageSizeChange,
}) {
  if (totalElements === 0) return null;

  const first = page * pageSize + 1;
  const last = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-grafito-700 px-4 py-3 sm:flex-row">
      <div className="flex items-center gap-3">
        <span className="text-sm text-grafito-300">
          {first}–{last} de {totalElements}
        </span>
        <label className="flex items-center gap-2 text-sm text-grafito-300">
          Resultados por página
          <select
            value={pageSize}
            onChange={(e) => onPageSizeChange?.(Number(e.target.value))}
            className="rounded-md border border-grafito-600 bg-grafito-900 px-2 py-1 text-sm text-grafito-100 outline-none focus:border-[#d4762a]"
          >
            {sizes.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-label="Página anterior"
          disabled={page === 0}
          onClick={() => onChange?.(page - 1)}
          className="rounded-md border border-grafito-600 px-3 py-1.5 text-sm text-grafito-200 transition-colors hover:bg-grafito-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          ‹
        </button>
        {Array.from({ length: totalPages }, (_, i) => (
          <button
            key={i}
            type="button"
            aria-label={`Ir a página ${i + 1}`}
            onClick={() => onChange?.(i)}
            className={`rounded-md border px-3 py-1.5 text-sm transition-colors ${
              i === page
                ? 'border-[#d4762a] bg-[#d4762a] text-white'
                : 'border-grafito-600 text-grafito-200 hover:bg-grafito-800'
            }`}
          >
            {i + 1}
          </button>
        ))}
        <button
          type="button"
          aria-label="Página siguiente"
          disabled={page >= totalPages - 1}
          onClick={() => onChange?.(page + 1)}
          className="rounded-md border border-grafito-600 px-3 py-1.5 text-sm text-grafito-200 transition-colors hover:bg-grafito-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          ›
        </button>
      </div>
    </div>
  );
}
