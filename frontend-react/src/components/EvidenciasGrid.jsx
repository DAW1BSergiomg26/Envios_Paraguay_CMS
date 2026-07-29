import { useState } from 'react';

export default function EvidenciasGrid({ evidencias = [] }) {
  if (!evidencias.length) {
    return (
      <div className="empty-state">
        <span className="empty-state-icon" style={{ fontSize: '2rem' }}>📷</span>
        <p className="empty-state-text">No hay evidencias registradas para este envío</p>
      </div>
    );
  }

  const [preview, setPreview] = useState(null);

  return (
    <div className="evidencias-section">
      <div className="evidencias-grid">
        {evidencias.map((ev, i) => (
          <div key={i} className="evidencia-card" onClick={() => setPreview(ev)}>
            <div className="evidencia-image-wrapper">
              <img
                src={ev.urlArchivo}
                alt={ev.titulo || 'Evidencia'}
                className="evidencia-image"
                loading="lazy"
              />
            </div>
            <div className="evidencia-info">
              <span className="evidencia-title">{ev.titulo || 'Sin título'}</span>
              {ev.descripcion && <span className="evidencia-desc">{ev.descripcion}</span>}
            </div>
          </div>
        ))}
      </div>

      {preview && (
        <div className="evidencia-modal-overlay" onClick={() => setPreview(null)}>
          <div className="evidencia-modal" onClick={e => e.stopPropagation()}>
            <button className="evidencia-modal-close" onClick={() => setPreview(null)}>✕</button>
            <img src={preview.urlArchivo} alt={preview.titulo || 'Evidencia'} className="evidencia-modal-img" />
            <div className="evidencia-modal-info">
              <strong>{preview.titulo || 'Sin título'}</strong>
              {preview.descripcion && <p>{preview.descripcion}</p>}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
