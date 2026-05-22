export default function OfflineBanner({ isOnline }) {
  if (isOnline) return null;

  return (
    <div className="offline-banner">
      <div className="offline-banner__content">
        <span className="offline-banner__icon">☁️</span>
        <div>
          <p className="offline-banner__title">Estás sin conexión</p>
          <p className="offline-banner__subtitle">Viendo últimos datos disponibles</p>
        </div>
      </div>
      <button className="offline-banner__retry" onClick={() => window.location.reload()}>Reintentar</button>
    </div>
  );
}
