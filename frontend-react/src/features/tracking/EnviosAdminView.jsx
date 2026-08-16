import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import usePolling from '../../hooks/usePolling';
import useWebSocket from '../../hooks/useWebSocket';
import { useToast } from '../../context/NotificationContext';
import { saveDashboardCache, getDashboardCache } from '../../services/offlineCache';
import { fetchEnvios, deleteEnvio } from './trackingService';
import { DEFAULT_PAGE_SIZE, ESTADO_LABELS } from './trackingConstants';
import EnvioFilters from './EnvioFilters';
import EnvioTable from './EnvioTable';
import ExportButtons from '../../components/ExportButtons';

const POLL_INTERVAL = 15000;

export default function EnviosAdminView({ onDataChange }) {
  const navigate = useNavigate();
  const { showError: showErrToast, showSuccess, showInfo } = useToast();
  const [envios, setEnvios] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [estados, setEstados] = useState([]);
  const [query, setQuery] = useState('');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');
  const [sessionOk, setSessionOk] = useState(true);

  const pageRef = useRef(page);
  const pageSizeRef = useRef(pageSize);
  const estadosRef = useRef(estados);
  const queryRef = useRef(query);
  const fechaDesdeRef = useRef(fechaDesde);
  const fechaHastaRef = useRef(fechaHasta);
  const enviosRef = useRef(envios);

  useEffect(() => { pageRef.current = page; }, [page]);
  useEffect(() => { pageSizeRef.current = pageSize; }, [pageSize]);
  useEffect(() => { estadosRef.current = estados; }, [estados]);
  useEffect(() => { queryRef.current = query; }, [query]);
  useEffect(() => { fechaDesdeRef.current = fechaDesde; }, [fechaDesde]);
  useEffect(() => { fechaHastaRef.current = fechaHasta; }, [fechaHasta]);
  useEffect(() => { enviosRef.current = envios; }, [envios]);

  const buildFilters = useCallback((p, ps, est, q, fd, fh) => ({
    page: p,
    size: ps,
    estados: est,
    query: q,
    fechaDesde: fd,
    fechaHasta: fh,
  }), []);

  const loadEnvios = useCallback(async (filters) => {
    setError(null);
    try {
      const res = await fetchEnvios(filters);
      setEnvios(res.data.content || []);
      setTotal(res.data.totalElements || 0);
      setTotalPages(res.data.totalPages || 0);
      setSessionOk(true);
      if (filters.page === 0 && filters.estados.length === 0 && !filters.query && !filters.fechaDesde && !filters.fechaHasta) {
        saveDashboardCache(res.data);
      }
    } catch (err) {
      const msg = err.message || 'Error de conexión';
      if (msg.toLowerCase().includes('sesión') || msg.toLowerCase().includes('login') || msg.toLowerCase().includes('denegado')) {
        setSessionOk(false);
      } else {
        const cached = getDashboardCache();
        if (cached) {
          setEnvios(cached.data.content || []);
          setTotal(cached.data.totalElements || 0);
          setTotalPages(cached.data.totalPages || 0);
          showInfo('Mostrando datos offline');
        } else {
          showErrToast(msg);
        }
      }
      setError(msg);
    }
  }, [showErrToast, showInfo]);

  const refreshFn = useCallback(async () => {
    await loadEnvios(buildFilters(
      pageRef.current,
      pageSizeRef.current,
      estadosRef.current,
      queryRef.current,
      fechaDesdeRef.current,
      fechaHastaRef.current,
    ));
  }, [loadEnvios, buildFilters]);

  const { polling, lastUpdated, refreshNow: baseRefresh, refreshError } = usePolling(refreshFn, POLL_INTERVAL, sessionOk);

  const refreshNow = useCallback(async () => {
    await baseRefresh();
    showSuccess('Datos actualizados');
  }, [baseRefresh, showSuccess]);

  const refreshFnRef = useRef(refreshFn);
  useEffect(() => { refreshFnRef.current = refreshFn; }, [refreshFn]);

  const handleMensajeWs = useCallback((msg) => {
    if (!msg || !msg.tracking || !msg.estado) return;
    const visible = enviosRef.current.some((e) => e.codigoUnico === msg.tracking);
    if (!visible) return;
    const etiqueta = ESTADO_LABELS[msg.estado] || msg.estado;
    setEnvios((prev) => prev.map((e) => (e.codigoUnico === msg.tracking ? { ...e, estado: msg.estado } : e)));
    refreshFnRef.current();
    showInfo(`Envío ${msg.tracking} actualizado a ${etiqueta}`);
  }, [showInfo]);

  useWebSocket({ onMessage: handleMensajeWs });

  const handleEliminar = useCallback(async (envio) => {
    if (!window.confirm(`¿Eliminar el envío ${envio.codigoUnico}?`)) return;
    try {
      await deleteEnvio(envio.codigoUnico);
      showSuccess('Envío eliminado');
      await refreshFn();
    } catch (err) {
      showErrToast(err.message || 'Error al eliminar el envío');
    }
  }, [refreshFn, showSuccess, showErrToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- patrón de data-fetching: loading al inicio de la carga
    setLoading(true);
    loadEnvios(buildFilters(page, pageSize, estados, query, fechaDesde, fechaHasta))
      .finally(() => setLoading(false));
  }, [page, pageSize, estados, query, fechaDesde, fechaHasta, loadEnvios, buildFilters]);

  useEffect(() => {
    onDataChange?.({ envios, total, totalPages, loading, error, sessionOk, lastUpdated, polling, refreshError, refreshNow });
  }, [envios, total, totalPages, loading, error, sessionOk, lastUpdated, polling, refreshError, refreshNow, onDataChange]);

  const handleSearch = useCallback((v) => { setQuery(v); setPage(0); }, []);
  const handleEstados = useCallback((v) => { setEstados(v); setPage(0); }, []);
  const handleFechaDesde = useCallback((v) => { setFechaDesde(v); setPage(0); }, []);
  const handleFechaHasta = useCallback((v) => { setFechaHasta(v); setPage(0); }, []);
  const handlePage = useCallback((p) => { setPage(p); }, []);
  const handlePageSize = useCallback((s) => { setPageSize(s); setPage(0); }, []);
  const handleRemoveEstado = useCallback((e) => { setEstados((prev) => prev.filter((s) => s !== e)); setPage(0); }, []);
  const handleClearQuery = useCallback(() => { setQuery(''); setPage(0); }, []);
  const handleClearFecha = useCallback(() => { setFechaDesde(''); setFechaHasta(''); setPage(0); }, []);
  const handleClearAll = useCallback(() => {
    setEstados([]);
    setQuery('');
    setFechaDesde('');
    setFechaHasta('');
    setPage(0);
  }, []);

  const hasFilters = Boolean(query || estados.length > 0 || fechaDesde || fechaHasta);

  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-grafito-700 bg-grafito-900/50 p-5">
        <EnvioFilters
          query={query}
          estados={estados}
          fechaDesde={fechaDesde}
          fechaHasta={fechaHasta}
          onQueryChange={handleSearch}
          onEstadosChange={handleEstados}
          onFechaDesdeChange={handleFechaDesde}
          onFechaHastaChange={handleFechaHasta}
          onRemoveEstado={handleRemoveEstado}
          onClearQuery={handleClearQuery}
          onClearFecha={handleClearFecha}
          onClearAll={handleClearAll}
        />
        <div className="mt-4 flex justify-end">
          <ExportButtons envios={envios} />
        </div>
      </section>

      <EnvioTable
        envios={envios}
        loading={loading}
        page={page}
        totalPages={totalPages}
        totalElements={total}
        pageSize={pageSize}
        hasFilters={hasFilters}
        onPageChange={handlePage}
        onPageSizeChange={handlePageSize}
        onEdit={(codigo) => navigate(`/dashboard/envios/${codigo}/editar`)}
        onDelete={handleEliminar}
        onRowClick={(codigo) => navigate(`/dashboard/envio/${codigo}`)}
      />
    </div>
  );
}
