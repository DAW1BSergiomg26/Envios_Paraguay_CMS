import { useState } from 'react';
import { descargarDocumento } from '../services/api';

export default function EvidenciasGrid({ evidencias = [] }) {
  const [preview, setPreview] = useState(null);

  if (!evidencias.length) {
    return (
      <div className="empty-state">
        <span className="empty-state-icon" style={{ fontSize: '2rem' }}>📷</span>
        <p className="empty-state-text">No hay evidencias registradas para este envío</p>
      </div>
    );
  }

  return (
    <div className="evidencias-section">
      <div className="evidencias-grid">
        {evidencias.map((ev, i) => (
          <div key={i} className="evidencia-card">
            <div className="evidencia-image-wrapper" onClick={() => setPreview(ev)}>
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
            <div className="evidencia-actions">
              <button
                type="button"
                className="evidencia-download"
                aria-label={`Descargar ${ev.titulo || 'evidencia'}`}
                onClick={() => descargarDocumento(ev.urlArchivo)}
              >
                ⬇ Descargar
              </button>
            </div>
          </div>
        ))}
      </div>

      {preview && (
        <div className="evidencia-modal-overlay" onClick={() => setPreview(null)}>
          <div className="evidencia-modal" role="dialog" aria-modal="true" onClick={e => e.stopPropagation()}>
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
