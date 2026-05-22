import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAdminEnvios } from '../services/api';
import usePolling from '../hooks/usePolling';
import { useToast } from '../context/NotificationContext';
import RefreshIndicator from '../components/RefreshIndicator';
import AnalyticsSection from '../components/AnalyticsSection';
import StatsCard from '../components/StatsCard';
import StatusBadge from '../components/StatusBadge';
import Pagination from '../components/Pagination';
import SearchBar from '../components/SearchBar';
import MultiStatusFilter from '../components/MultiStatusFilter';
import DateRangeFilter from '../components/DateRangeFilter';
import ActiveFilters from '../components/ActiveFilters';
import ExportButtons from '../components/ExportButtons';
import EmptyState from '../components/EmptyState';
import { SkeletonRow, SkeletonCard } from '../components/SkeletonLoader';

const PAGE_SIZE = 10;
const POLL_INTERVAL = 15000;

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { showError: showErrToast, showWarning, showSuccess } = useToast();
  const [envios, setEnvios] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [estados, setEstados] = useState([]);
  const [query, setQuery] = useState('');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');
  const [sessionOk, setSessionOk] = useState(true);

  const pageRef = useRef(page);
  const estadosRef = useRef(estados);
  const queryRef = useRef(query);
  const fechaDesdeRef = useRef(fechaDesde);
  const fechaHastaRef = useRef(fechaHasta);

  useEffect(() => { pageRef.current = page; }, [page]);
  useEffect(() => { estadosRef.current = estados; }, [estados]);
  useEffect(() => { queryRef.current = query; }, [query]);
  useEffect(() => { fechaDesdeRef.current = fechaDesde; }, [fechaDesde]);
  useEffect(() => { fechaHastaRef.current = fechaHasta; }, [fechaHasta]);

  const buildParams = useCallback((p, est, q, fd, fh) => {
    const params = { page: p, size: PAGE_SIZE };
    if (est && est.length > 0) params.estados = est;
    if (q) params.q = q;
    if (fd) params.fechaDesde = fd;
    if (fh) params.fechaHasta = fh;
    return params;
  }, []);

  const fetchEnvios = useCallback(async (p, est, q, fd, fh) => {
    setError(null);
    try {
      const params = buildParams(p, est, q, fd, fh);
      const res = await getAdminEnvios(params);
      setEnvios(res.data.content || []);
      setTotal(res.data.totalElements || 0);
      setTotalPages(res.data.totalPages || 0);
      setSessionOk(true);
    } catch (err) {
      const msg = err.message || 'Error de conexión';
      if (msg.toLowerCase().includes('sesión') || msg.toLowerCase().includes('login') || msg.toLowerCase().includes('denegado')) {
        setSessionOk(false);
      } else {
        showErrToast(msg);
      }
      setError(msg);
    }
  }, [buildParams, showErrToast]);

  const refreshFn = useCallback(async () => {
    await fetchEnvios(pageRef.current, estadosRef.current, queryRef.current, fechaDesdeRef.current, fechaHastaRef.current);
  }, [fetchEnvios]);

  const { polling, lastUpdated, refreshNow: baseRefresh, refreshError } = usePolling(refreshFn, POLL_INTERVAL, sessionOk);

  const refreshNow = useCallback(async () => {
    await baseRefresh();
    showSuccess('Datos actualizados');
  }, [baseRefresh, showSuccess]);

  useEffect(() => {
    setLoading(true);
    fetchEnvios(page, estados, query, fechaDesde, fechaHasta).finally(() => setLoading(false));
  }, [page, estados, query, fechaDesde, fechaHasta, fetchEnvios]);

  const handleSearch = useCallback((v) => { setQuery(v); setPage(0); }, []);

  const handleEstados = useCallback((v) => { setEstados(v); setPage(0); }, []);

  const handleFechaDesde = useCallback((v) => { setFechaDesde(v); setPage(0); }, []);

  const handleFechaHasta = useCallback((v) => { setFechaHasta(v); setPage(0); }, []);

  const handlePage = useCallback((p) => { setPage(p); }, []);

  const handleRemoveEstado = useCallback((e) => {
    setEstados(prev => prev.filter(s => s !== e));
    setPage(0);
  }, []);

  const handleClearQuery = useCallback(() => { setQuery(''); setPage(0); }, []);

  const handleClearFecha = useCallback(() => { setFechaDesde(''); setFechaHasta(''); setPage(0); }, []);

  const handleClearAll = useCallback(() => {
    setEstados([]);
    setQuery('');
    setFechaDesde('');
    setFechaHasta('');
    setPage(0);
  }, []);

  const stats = [
    { label: 'Total envíos', value: total, icon: '📦', color: '#3b82f6' },
    { label: 'En tránsito', value: envios.filter(e => e.estado === 'EN_TRANSITO').length, icon: '🚢', color: '#8b5cf6' },
    { label: 'Entregados', value: envios.filter(e => e.estado === 'ENTREGADO').length, icon: '✅', color: '#10b981' },
    { label: 'En aduana', value: envios.filter(e => e.estado?.includes('ADUANA')).length, icon: '🛃', color: '#f59e0b' },
    { label: 'Pendientes', value: envios.filter(e => e.estado === 'RECIBIDO').length, icon: '📋', color: '#6b7280' }
  ];

  const needsLogin = !sessionOk;

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Panel de Envíos</h1>
          <p className="dashboard-subtitle">Gestión de tracking internacional España ↔ Paraguay</p>
        </div>
        <RefreshIndicator lastUpdated={lastUpdated} polling={polling} refreshError={refreshError} onRefresh={refreshNow} />
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

      <section className="toolbar">
        <SearchBar value={query} onChange={handleSearch} placeholder="Buscar por código, destinatario, origen…" />
        <DateRangeFilter fechaDesde={fechaDesde} fechaHasta={fechaHasta} onChangeDesde={handleFechaDesde} onChangeHasta={handleFechaHasta} />
        <ExportButtons envios={envios} />
      </section>

      <section className="toolbar toolbar--chips">
        <MultiStatusFilter selected={estados} onChange={handleEstados} />
      </section>

      <ActiveFilters
        estados={estados}
        query={query}
        fechaDesde={fechaDesde}
        fechaHasta={fechaHasta}
        onRemoveEstado={handleRemoveEstado}
        onClearQuery={handleClearQuery}
        onClearFecha={handleClearFecha}
        onClearAll={handleClearAll}
      />

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
          <EmptyState message={query || estados.length > 0 || fechaDesde || fechaHasta ? 'No se encontraron envíos con esos filtros' : 'No hay envíos registrados'} />
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
                  <tr key={e.codigoUnico} className="envio-row" onClick={() => navigate(`/dashboard/envio/${e.codigoUnico}`)}>
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
