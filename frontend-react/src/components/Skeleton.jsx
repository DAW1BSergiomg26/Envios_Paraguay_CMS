import { useMemo } from 'react';

/**
 * TableSkeleton - Esqueleto de carga para tablas de datos
 * Usa animaciones shimmer/pulse suaves con Tailwind CSS
 */
export function TableSkeleton({
  rows = 6,
  columns = 6,
  columnWidths,
  showHeader = true,
  className = '',
}) {
  const columnStyles = useMemo(() => {
    if (!columnWidths) return null;
    return columnWidths.map((w) => ({
      width: typeof w === 'number' ? `${w}%` : w,
      minWidth: typeof w === 'number' ? `${w}%` : w,
    }));
  }, [columnWidths]);

  const skeletonClasses = 'bg-grafito-800/50 animate-pulse-soft rounded';

  return (
    <div className={`overflow-hidden rounded-lg border border-grafito-700 bg-grafito-900/50 ${className}`}>
      {/* Header skeleton */}
      {showHeader && (
        <div className="bg-grafito-950 border-b border-grafito-700">
          <div className="grid gap-4 p-4" style={columnStyles ? { gridTemplateColumns: columnWidths.map((w) => typeof w === 'number' ? `${w}%` : w).join(' ') } : { gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
            {Array.from({ length: columns }, (_, i) => (
              <div key={`header-${i}`} className={`${skeletonClasses} h-4 w-full`} style={columnStyles?.[i]} />
            ))}
          </div>
        </div>
      )}

      {/* Body skeleton rows */}
      <div className="divide-y divide-grafito-700">
        {Array.from({ length: rows }, (_, rowIndex) => (
          <div key={`row-${rowIndex}`} className="grid gap-4 p-4 transition-colors hover:bg-grafito-800/50" style={columnStyles ? { gridTemplateColumns: columnWidths.map((w) => typeof w === 'number' ? `${w}%` : w).join(' ') } : { gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
            {Array.from({ length: columns }, (_, colIndex) => (
              <div
                key={`cell-${rowIndex}-${colIndex}`}
                className={skeletonClasses}
                style={{
                  ...columnStyles?.[colIndex],
                  height: colIndex === 0 ? '1.125rem' : '0.875rem',
                  width: colIndex === 0 ? '60%' : undefined,
                }}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * TableRowSkeleton - Una sola fila de esqueleto para listas
 */
export function TableRowSkeleton({
  columns = 5,
  columnWidths,
  variant = 'default',
  className = '',
}) {
  const baseClasses = 'animate-pulse-soft rounded';
  const variants = {
    default: 'bg-grafito-800/50',
    card: 'bg-grafito-700/50',
    light: 'bg-grafito-600/30',
  };

  return (
    <div className={`grid gap-4 p-4 ${className}`} style={columnWidths ? { gridTemplateColumns: columnWidths.map((w) => typeof w === 'number' ? `${w}%` : w).join(' ') } : { gridTemplateColumns: `repeat(${columns}, 1fr)` }}>
      {Array.from({ length: columns }, (_, i) => (
        <div
          key={i}
          className={`${variants[variant]} ${baseClasses}`}
          style={{
            ...columnWidths?.[i],
            height: i === 0 ? '1.125rem' : '0.875rem',
            width: i === 0 ? '60%' : undefined,
          }}
        />
      ))}
    </div>
  );
}

/**
 * CardSkeleton - Esqueleto para tarjetas (KPIs, stats, etc)
 */
export function CardSkeleton({
  variant = 'default',
  className = '',
}) {
  const variants = {
    default: 'bg-grafito-800/50 border-grafito-700',
    kpi: 'bg-grafito-800/50 border-grafito-700',
    chart: 'bg-grafito-800/50 border-grafito-700',
    light: 'bg-grafito-700/30 border-grafito-600',
  };

  return (
    <div className={`animate-pulse-soft rounded-xl border p-4 ${variants[variant]} ${className}`}>
      <div className="flex items-center gap-3 mb-4">
        <div className="w-10 h-10 rounded-lg bg-grafito-700/50 animate-pulse-soft" />
        <div className="flex-1">
          <div className="h-4 w-1/3 bg-grafito-700/50 rounded animate-pulse-soft" />
          <div className="h-3 w-1/4 bg-grafito-600/50 rounded animate-pulse-soft mt-1" />
        </div>
      </div>
      <div className="space-y-2">
        <div className="h-8 w-1/2 bg-grafito-700/50 rounded animate-pulse-soft" />
        <div className="h-4 w-1/3 bg-grafito-600/50 rounded animate-pulse-soft" />
      </div>
    </div>
  );
}

/**
 * ListSkeleton - Esqueleto para listas de elementos
 */
export function ListSkeleton({
  items = 5,
  showAvatar = true,
  showMeta = true,
  className = '',
}) {
  return (
    <div className={`space-y-3 ${className}`}>
      {Array.from({ length: items }, (_, i) => (
        <div key={i} className="flex items-center gap-3 p-3 bg-grafito-800/50 rounded-lg border border-grafito-700 animate-pulse-soft">
          {showAvatar && (
            <div className="w-10 h-10 rounded-full bg-grafito-700/50 animate-pulse-soft flex-shrink-0" />
          )}
          <div className="flex-1 min-w-0 space-y-1">
            <div className="h-4 w-1/3 bg-grafito-700/50 rounded animate-pulse-soft" />
            {showMeta && (
              <div className="h-3 w-1/4 bg-grafito-600/50 rounded animate-pulse-soft" />
            )}
          </div>
          <div className="w-16 h-6 bg-grafito-700/50 rounded animate-pulse-soft" />
        </div>
      ))}
    </div>
  );
}

/**
 * DetailSkeleton - Esqueleto para páginas de detalle
 */
export function DetailSkeleton({ className = '' }) {
  return (
    <div className={`space-y-6 ${className}`}>
      {/* Hero section skeleton */}
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 pb-4 border-b border-grafito-700 animate-pulse-soft">
        <div className="flex-1">
          <div className="h-8 w-1/3 bg-grafito-700/50 rounded animate-pulse-soft mb-2" />
          <div className="h-4 w-1/4 bg-grafito-600/50 rounded animate-pulse-soft" />
        </div>
        <div className="h-10 w-32 bg-grafito-700/50 rounded-full animate-pulse-soft" />
      </div>

      {/* Info cards grid skeleton */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }, (_, i) => (
          <CardSkeleton key={i} variant="kpi" />
        ))}
      </div>

      {/* Section skeleton */}
      <div className="space-y-4 pt-4 border-t border-grafito-700">
        <div className="h-5 w-1/4 bg-grafito-600/50 rounded animate-pulse-soft" />
        <div className="grid gap-4 lg:grid-cols-2">
          {Array.from({ length: 2 }, (_, i) => (
            <div key={i} className="bg-grafito-800/50 border border-grafito-700 rounded-xl p-4 animate-pulse-soft space-y-3">
              <div className="h-5 w-1/4 bg-grafito-600/50 rounded animate-pulse-soft" />
              <div className="h-32 bg-grafito-700/50 rounded-lg animate-pulse-soft" />
            </div>
          ))}
        </div>

        {/* Timeline skeleton */}
        <div className="pl-10 border-l border-grafito-700 space-y-6">
          {Array.from({ length: 5 }, (_, i) => (
            <div key={i} className="relative pb-6 animate-pulse-soft">
              <div className="absolute left-[-1.75rem] top-0 w-6 h-6 rounded-full bg-grafito-700/50 border-2 border-grafito-600 animate-pulse-soft" />
              <div className="h-4 w-1/3 bg-grafito-700/50 rounded animate-pulse-soft mb-1" />
              <div className="h-3 w-1/2 bg-grafito-600/50 rounded animate-pulse-soft" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/**
 * ChartSkeleton - Esqueleto para áreas de gráficos
 */
export function ChartSkeleton({
  height = 200,
  showLegend = true,
  className = '',
}) {
  return (
    <div className={`bg-grafito-800/50 border border-grafito-700 rounded-xl p-4 animate-pulse-soft ${className}`} style={{ minHeight: `${height}px` }}>
      <div className="h-5 w-1/4 bg-grafito-600/50 rounded animate-pulse-soft mb-4" />
      <div className="h-full bg-grafito-700/30 rounded-lg animate-pulse-soft flex items-center justify-center">
        <div className="w-full h-full bg-gradient-to-r from-grafito-700/50 via-grafito-800/50 to-grafito-700/50 bg-[length:200%_100%] animate-[shimmer_2s_linear_infinite] rounded-lg" />
      </div>
      {showLegend && (
        <div className="flex flex-wrap gap-2 justify-center mt-4 animate-pulse-soft">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="flex items-center gap-1.5 h-5 w-24 bg-grafito-700/50 rounded animate-pulse-soft" />
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * ShimmerSkeleton - Componente base de shimmer reutilizable
 */
export function ShimmerSkeleton({
  className = '',
  style,
  ...props
}) {
  return (
    <div
      className={`animate-[shimmer_2s_linear_infinite] bg-gradient-to-r from-grafito-700/50 via-grafito-800/50 to-grafito-700/50 bg-[length:200%_100%] rounded ${className}`}
      style={style}
      {...props}
    />
  );
}

export default TableSkeleton;