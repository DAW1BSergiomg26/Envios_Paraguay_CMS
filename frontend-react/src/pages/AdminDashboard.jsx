import { useState, useEffect, useCallback } from 'react';
import { getAdminEnvios } from '../services/api';
import StatsCard from '../components/StatsCard';
import StatusBadge from '../components/StatusBadge';
import Pagination from '../components/Pagination';
import SearchBar from '../components/SearchBar';
import StatusFilter from '../components/StatusFilter';
import EmptyState from '../components/EmptyState';
import { SkeletonRow, SkeletonCard } from '../components/SkeletonLoader';

const PAGE_SIZE = 10;

export default function AdminDashboard() {
  const [envios, setEnvios] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [estado, setEstado] = useState('');
  const [codigo, setCodigo] = useState('');

  const fetchEnvios = useCallback(async (p, e, c) => {
    setLoading(true);
    setError(null);
    try {
      const params = { page: p, size: PAGE_SIZE };
      if (e) params.estado = e;
      if (c) params.codigo = c;
      const res = await getAdminEnvios(params);
      setEnvios(res.data.content || []);
      setTotal(res.data.totalElements || 0);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      setError(err.message || 'Error de conexión');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchEnvios(page, estado, codigo); }, [page, estado, codigo, fetchEnvios]);

  const handleSearch = useCallback((v) => { setCodigo(v); setPage(0); }, []);
  const handleEstado = useCallback((v) => { setEstado(v); setPage(0); }, []);
  const handlePage = useCallback((p) => { setPage(p); }, []);

  const stats = [
    { label: 'Total envíos', value: total, icon: '📦', color: '#3b82f6' },
    { label: 'En tránsito', value: envios.filter(e => e.estado === 'EN_TRANSITO').length, icon: '🚢', color: '#8b5cf6' },
    { label: 'Entregados', value: envios.filter(e => e.estado === 'ENTREGADO').length, icon: '✅', color: '#10b981' },
    { label: 'En aduana', value: envios.filter(e => e.estado?.includes('ADUANA')).length, icon: '🛃', color: '#f59e0b' },
    { label: 'Pendientes', value: envios.filter(e => e.estado === 'RECIBIDO').length, icon: '📋', color: '#6b7280' }
  ];

  const needsLogin = error && (
    error.toLowerCase().includes('sesión') ||
    error.toLowerCase().includes('login') ||
    error.toLowerCase().includes('denegado')
  );

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Panel de Envíos</h1>
          <p className="dashboard-subtitle">Gestión de tracking internacional España ↔ Paraguay</p>
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

      <section className="toolbar">
        <SearchBar value={codigo} onChange={handleSearch} placeholder="Buscar por código de tracking…" />
        <StatusFilter value={estado} onChange={handleEstado} />
      </section>

      <section className="table-section">
        <div className="table-header">
          <h2>Envíos</h2>
          <span className="table-count">{total} registro{total !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Código</th><th>Estado</th><th>Destinatario</th><th>Origen</th><th>Destino</th><th>Fecha</th>
              </tr>
            </thead>
            <tbody>
              {Array.from({ length: 5 }, (_, i) => <SkeletonRow key={i} />)}
            </tbody>
          </table>
        ) : envios.length === 0 ? (
          <EmptyState message={codigo || estado ? 'No se encontraron envíos con esos filtros' : 'No hay envíos registrados'} />
        ) : (
          <>
            <table className="envios-table">
              <thead>
                <tr>
                  <th>Código</th><th>Estado</th><th>Destinatario</th><th>Origen</th><th>Destino</th><th>Fecha</th>
                </tr>
              </thead>
              <tbody>
                {envios.map(e => (
                  <tr key={e.codigoUnico} className="envio-row">
                    <td className="cell-code">{e.codigoUnico}</td>
                    <td><StatusBadge estado={e.estado} /></td>
                    <td>{e.destinatario}</td>
                    <td>{e.origen}</td>
                    <td>{e.destino}</td>
                    <td className="cell-date">{new Date(e.ultimaActualizacion).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={page} totalPages={totalPages} totalElements={total} pageSize={PAGE_SIZE} onChange={handlePage} />
          </>
        )}
      </section>
    </div>
  );
}
