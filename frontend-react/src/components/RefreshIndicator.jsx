import { useState, useEffect } from 'react';

export default function RefreshIndicator({ lastUpdated, polling, refreshError, onRefresh }) {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    if (!lastUpdated) { setElapsed(0); return; }
    const tick = () => setElapsed(Math.floor((Date.now() - lastUpdated) / 1000));
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [lastUpdated]);

  return (
    <div className="refresh-indicator">
      {polling ? (
        <span className="refresh-status refresh-status--syncing">
          <span className="refresh-spinner" />
          Sincronizando…
        </span>
      ) : refreshError ? (
        <span className="refresh-status refresh-status--error" onClick={onRefresh}>
          Error al actualizar. <button className="refresh-btn">Reintentar</button>
        </span>
      ) : lastUpdated ? (
        <span className="refresh-status refresh-status--idle">
          Actualizado hace {elapsed < 5 ? 'unos segundos' : `${elapsed} segundos`}
          <button className="refresh-btn" onClick={onRefresh}>Actualizar ahora</button>
        </span>
      ) : (
        <span className="refresh-status refresh-status--idle">
          <button className="refresh-btn" onClick={onRefresh}>Cargar datos</button>
        </span>
      )}
    </div>
  );
}
