import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import AppleHero from '../components/AppleHero';
import RefreshIndicator from '../components/RefreshIndicator';
import AnalyticsSection from '../components/AnalyticsSection';
import StatsCard from '../components/StatsCard';
import OfflineBanner from '../components/OfflineBanner';
import { SkeletonCard } from '../components/SkeletonLoader';
import EnviosAdminView from '../features/tracking/EnviosAdminView';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const isOnline = useOnlineStatus();
  const [data, setData] = useState(null);

  const envios = data?.envios || [];
  const total = data?.total || 0;
  const loading = data?.loading ?? true;
  const error = data?.error || null;
  const sessionOk = data?.sessionOk ?? true;
  const lastUpdated = data?.lastUpdated || null;
  const polling = data?.polling || false;
  const refreshError = data?.refreshError || null;
  const refreshNow = data?.refreshNow || null;

  const needsLogin = !sessionOk;

  const stats = [
    { label: 'Total envíos', value: total, icon: '📦', color: '#3b82f6' },
    { label: 'En tránsito', value: envios.filter(e => e.estado === 'EN_TRANSITO').length, icon: '🚢', color: '#8b5cf6' },
    { label: 'Entregados', value: envios.filter(e => e.estado === 'ENTREGADO').length, icon: '✅', color: '#10b981' },
    { label: 'En aduana', value: envios.filter(e => e.estado?.includes('ADUANA')).length, icon: '🛃', color: '#f59e0b' },
    { label: 'Pendientes', value: envios.filter(e => e.estado === 'RECIBIDO').length, icon: '📋', color: '#6b7280' }
  ];

  return (
    <div className="dashboard">
      <OfflineBanner isOnline={isOnline} />
      <AppleHero />
      <header className="dashboard-header">
        <div>
          <h1>Panel de Envíos</h1>
          <p className="dashboard-subtitle">Gestión de tracking internacional España ↔ Paraguay</p>
        </div>
        <div className="dashboard-header-actions">
          <button type="button" className="btn-nuevo" onClick={() => navigate('/dashboard/envios/nuevo')}>＋ Nuevo envío</button>
          <RefreshIndicator lastUpdated={lastUpdated} polling={polling} refreshError={refreshError} onRefresh={refreshNow} />
        </div>
      </header>

      {error && needsLogin && (
        <div className="error-banner error-banner--login">
          <strong>Se requiere autenticación</strong>
          <p>{error}</p>
        </div>
      )}
      {error && !needsLogin && (
        <div className="error-banner">{error}</div>
      )}

      <section className="stats-grid">
        {loading
          ? Array.from({ length: 5 }, (_, i) => <SkeletonCard key={i} />)
          : stats.map((s, i) => <StatsCard key={i} {...s} />)
        }
      </section>

      <AnalyticsSection envios={envios} loading={loading} />

      <EnviosAdminView onDataChange={setData} />
    </div>
  );
}
