import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAdminEnvioDetalle } from '../services/api';
import usePolling from '../hooks/usePolling';
import { useToast } from '../context/NotificationContext';
import RefreshIndicator from '../components/RefreshIndicator';
import UpdateEstadoPanel from '../components/UpdateEstadoPanel';
import { parseLocalDateTime } from '../services/dateUtils';
import StatusBadge from '../components/StatusBadge';
import Timeline from '../components/Timeline';
import EvidenciasGrid from '../components/EvidenciasGrid';

const POLL_INTERVAL = 20000;

function DetailSkeleton() {
  return (
    <div className="detail-skeleton">
      <div className="skeleton skeleton--lg" style={{ height: '24px', width: '200px', marginBottom: '1rem' }} />
      <div className="detail-cards-grid">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="skeleton-card">
            <div className="skeleton skeleton--icon" />
            <div className="skeleton-card-body">
              <div className="skeleton skeleton--sm" style={{ marginBottom: '4px' }} />
              <div className="skeleton skeleton--md" />
            </div>
          </div>
        ))}
      </div>
      <div className="skeleton" style={{ height: '12px', width: '120px', margin: '2rem 0 1rem' }} />
      <div className="detail-cards-grid" style={{ gridTemplateColumns: '1fr' }}>
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="skeleton" style={{ height: '48px', width: '100%', marginBottom: '8px' }} />
        ))}
      </div>
      <div className="skeleton" style={{ height: '12px', width: '120px', margin: '2rem 0 1rem' }} />
      <div className="evidencias-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="skeleton" style={{ height: '140px', width: '100%', borderRadius: '8px' }} />
        ))}
      </div>
    </div>
  );
}

const STATUS_ICONS = {
  RECIBIDO: '📋',
  EN_ADUANA_ORIGEN: '🛃',
  EN_TRANSITO: '🚢',
  EN_ADUANA_DESTINO: '🛃',
  EN_REPARTO: '🚚',
  ENTREGADO: '✅'
};

const STATUS_COLORS = {
  RECIBIDO: '#6b7280',
  EN_ADUANA_ORIGEN: '#f59e0b',
  EN_TRANSITO: '#3b82f6',
  EN_ADUANA_DESTINO: '#f59e0b',
  EN_REPARTO: '#8b5cf6',
  ENTREGADO: '#10b981'
};

export default function ShipmentDetailPage() {
  const { codigo } = useParams();
  const navigate = useNavigate();
  const { showSuccess, showError: showErrToast } = useToast();
  const [envio, setEnvio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const estadoRef = useRef(null);

  const refreshFn = useCallback(async () => {
    const res = await getAdminEnvioDetalle(codigo);
    setEnvio(res.data);
  }, [codigo]);

  const { polling, lastUpdated, refreshNow: baseRefresh, refreshError } = usePolling(refreshFn, POLL_INTERVAL, !loading && !error);

  const refreshNow = useCallback(async () => {
    await baseRefresh();
    showSuccess('Datos actualizados');
  }, [baseRefresh, showSuccess]);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getAdminEnvioDetalle(codigo)
      .then(res => {
        setEnvio(res.data);
        estadoRef.current = res.data.estado;
      })
      .catch(err => {
        if (err.response?.status === 404) {
          setError('404');
        } else {
          const msg = err.message || 'Error de conexión';
          setError(msg);
          showErrToast(msg);
        }
      })
      .finally(() => setLoading(false));
  }, [codigo, showErrToast]);

  if (loading) {
    return (
      <div className="detail-page">
        <DetailSkeleton />
      </div>
    );
  }

  if (error === '404') {
    return (
      <div className="detail-page">
        <div className="detail-error">
          <span style={{ fontSize: '3rem' }}>🔍</span>
          <h2>Envío no encontrado</h2>
          <p>El código <strong>{codigo}</strong> no corresponde a ningún envío registrado</p>
          <button className="btn-back" onClick={() => navigate('/')}>Volver al dashboard</button>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="detail-page">
        <div className="error-banner">{error}</div>
        <button className="btn-back" onClick={() => navigate('/')}>Volver al dashboard</button>
      </div>
    );
  }

  if (!envio) return null;

  const estadoColor = STATUS_COLORS[envio.estado] || '#6b7280';
  const estadoIcon = STATUS_ICONS[envio.estado] || '📌';

  return (
    <div className="detail-page">
      <div className="detail-topbar">
        <button className="btn-back" onClick={() => navigate('/')}>← Volver al dashboard</button>
        <RefreshIndicator lastUpdated={lastUpdated} polling={polling} refreshError={refreshError} onRefresh={refreshNow} />
      </div>

      <div className="detail-hero">
        <div className="detail-hero-main">
          <h1 className="detail-code">{envio.codigoUnico}</h1>
          <div
            className="detail-status-badge-lg"
            style={{
              background: estadoColor + '15',
              borderColor: estadoColor + '50',
              color: estadoColor
            }}
          >
            <span className="detail-status-icon">{estadoIcon}</span>
            <span className="detail-status-label">{envio.estado?.replace(/_/g, ' ') || 'N/A'}</span>
          </div>
        </div>
        <p className="detail-hero-sub">
          Última actualización: {parseLocalDateTime(envio.ultimaActualizacion).toLocaleDateString('es-ES', {
            day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
          })}
        </p>
      </div>

      <UpdateEstadoPanel codigo={envio.codigoUnico} estadoActual={envio.estado} onUpdated={refreshNow} />

      <section>
        <h2 className="section-title">Información del envío</h2>
        <div className="detail-cards-grid">
          <div className="info-card">
            <span className="info-card-icon">📍</span>
            <span className="info-card-label">Origen</span>
            <span className="info-card-value">{envio.origen || '—'}</span>
          </div>
          <div className="info-card">
            <span className="info-card-icon">🎯</span>
            <span className="info-card-label">Destino</span>
            <span className="info-card-value">{envio.destino || '—'}</span>
          </div>
          <div className="info-card">
            <span className="info-card-icon">👤</span>
            <span className="info-card-label">Destinatario</span>
            <span className="info-card-value">{envio.destinatario || '—'}</span>
          </div>
          <div className="info-card">
            <span className="info-card-icon">📦</span>
            <span className="info-card-label">Peso</span>
            <span className="info-card-value">{envio.peso || '—'}</span>
          </div>
          {envio.contenido && (
            <div className="info-card">
              <span className="info-card-icon">📋</span>
              <span className="info-card-label">Contenido</span>
              <span className="info-card-value">{envio.contenido}</span>
            </div>
          )}
        </div>
      </section>

      {envio.clienteNombre && (
        <section>
          <h2 className="section-title">Cliente</h2>
          <div className="detail-cards-grid">
            <div className="info-card">
              <span className="info-card-icon">👤</span>
              <span className="info-card-label">Nombre</span>
              <span className="info-card-value">{envio.clienteNombre}</span>
            </div>
            <div className="info-card">
              <span className="info-card-icon">✉️</span>
              <span className="info-card-label">Email</span>
              <span className="info-card-value">{envio.clienteEmail}</span>
            </div>
          </div>
        </section>
      )}

      <section>
        <h2 className="section-title">Timeline de tracking</h2>
        <Timeline eventos={envio.eventos} />
      </section>

      <section>
        <h2 className="section-title">Evidencias</h2>
        <EvidenciasGrid evidencias={envio.evidencias} />
      </section>
    </div>
  );
}
