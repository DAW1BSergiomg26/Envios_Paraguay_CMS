export default function Pagination({ page, totalPages, totalElements, pageSize, onChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination">
      <span className="pagination-info">
        {page * pageSize + 1}–{Math.min((page + 1) * pageSize, totalElements)} de {totalElements}
      </span>
      <div className="pagination-controls">
        <button className="pagination-btn" disabled={page === 0} onClick={() => onChange(page - 1)}>
          ← Anterior
        </button>
        {Array.from({ length: totalPages }, (_, i) => (
          <button
            key={i}
            className={`pagination-btn pagination-page ${i === page ? 'active' : ''}`}
            onClick={() => onChange(i)}
          >
            {i + 1}
          </button>
        ))}
        <button className="pagination-btn" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
          Siguiente →
        </button>
      </div>
    </div>
  );
}
