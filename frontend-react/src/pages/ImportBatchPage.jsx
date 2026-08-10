import { useState, useEffect, useCallback, useRef } from 'react';
import {
  getAdminImports,
  getAdminClientes,
  getAdminImporte,
  getImportErrores,
  uploadImportCsv
} from '../services/api';
import usePolling from '../hooks/usePolling';
import { useToast } from '../context/NotificationContext';
import RefreshIndicator from '../components/RefreshIndicator';
import EmptyState from '../components/EmptyState';

const REFRESH_INTERVAL = 15000;
const LOTE_POLL_INTERVAL = 2000;
const MAX_FILE_BYTES = 5 * 1024 * 1024;
const TERMINALES = ['COMPLETADO', 'COMPLETADO_CON_ERRORES', 'FALLIDO'];

const ESTADO_CLASES = {
  PROCESANDO: 'lote-badge lote-badge--info',
  COMPLETADO: 'lote-badge lote-badge--success',
  COMPLETADO_CON_ERRORES: 'lote-badge lote-badge--warning',
  FALLIDO: 'lote-badge lote-badge--danger'
};

export default function ImportBatchPage() {
  const { showSuccess, showError } = useToast();
  const [lotes, setLotes] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [archivo, setArchivo] = useState(null);
  const [clienteId, setClienteId] = useState('');
  const [subiendo, setSubiendo] = useState(false);
  const [loteActivo, setLoteActivo] = useState(null);
  const [errores, setErrores] = useState([]);
  const [modalAbierto, setModalAbierto] = useState(false);
  const pollRef = useRef(null);

  const fetchLotes = useCallback(async () => {
    const res = await getAdminImports();
    setLotes(res.data || []);
  }, []);

  const { polling, lastUpdated, refreshNow, refreshError } = usePolling(fetchLotes, REFRESH_INTERVAL);

  useEffect(() => {
    getAdminClientes()
      .then(res => setClientes(res.data || []))
      .catch(err => showError(err.message || 'Error al cargar los clientes'));
  }, [showError]);

  useEffect(() => {
    fetchLotes().catch(err => showError(err.message || 'Error al cargar los lotes'));
  }, [fetchLotes, showError]);

  const pollLote = useCallback((id) => {
    getAdminImporte(id)
      .then(res => {
        const lote = res.data;
        setLoteActivo(lote);
        if (!TERMINALES.includes(lote.estado)) {
          pollRef.current = setTimeout(() => pollLote(id), LOTE_POLL_INTERVAL);
        } else {
          fetchLotes().catch(() => {});
        }
      })
      .catch(() => {
        pollRef.current = setTimeout(() => pollLote(id), LOTE_POLL_INTERVAL);
      });
  }, [fetchLotes]);

  useEffect(() => {
    return () => {
      if (pollRef.current) clearTimeout(pollRef.current);
    };
  }, []);

  const handleArchivo = useCallback((file) => {
    if (!file) {
      setArchivo(null);
      return;
    }
    if (!file.name.toLowerCase().endsWith('.csv')) {
      showError('El fichero debe tener extensión .csv');
      setArchivo(null);
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      showError('El fichero supera el tamaño máximo de 5 MB');
      setArchivo(null);
      return;
    }
    setArchivo(file);
  }, [showError]);

  const handleImportar = useCallback(async () => {
    if (!archivo) return;
    setSubiendo(true);
    try {
      const res = await uploadImportCsv(archivo, clienteId || null);
      const lote = res.data;
      setLoteActivo(lote);
      setArchivo(null);
      const fileInput = document.getElementById('csvFile');
      if (fileInput) fileInput.value = '';
      showSuccess(`Lote #${lote.id} iniciado`);
      pollLote(lote.id);
    } catch (err) {
      showError(err.message || 'Error al subir el CSV');
    } finally {
      setSubiendo(false);
    }
  }, [archivo, clienteId, pollLote, showSuccess, showError]);

  const handleVerErrores = useCallback(async (id) => {
    try {
      const res = await getImportErrores(id);
      setErrores(res.data || []);
      setModalAbierto(true);
    } catch (err) {
      showError(err.message || 'Error al cargar los errores del lote');
    }
  }, [showError]);

  const pct = loteActivo
    ? (loteActivo.totalRegistros > 0 ? loteActivo.totalRegistros : loteActivo.procesados) > 0
      ? Math.round((loteActivo.procesados / (loteActivo.totalRegistros > 0 ? loteActivo.totalRegistros : loteActivo.procesados)) * 100)
      : 0
    : 0;

  const nombreCliente = (id) => {
    const c = clientes.find(cl => cl.id === id);
    return c ? c.nombre : (id ? `Cliente ${id}` : 'Sin asignar');
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Carga masiva de envíos (CSV)</h1>
          <p className="dashboard-subtitle">Sube un CSV para registrar envíos en lote. El procesamiento es asíncrono.</p>
        </div>
        <RefreshIndicator lastUpdated={lastUpdated} polling={polling} refreshError={refreshError} onRefresh={refreshNow} />
      </header>

      <section className="import-card">
        <label className="dropzone" htmlFor="csvFile">
          <p className="dropzone-title">{archivo ? `✓ ${archivo.name}` : 'Arrastra tu CSV aquí o haz clic para seleccionar'}</p>
          <small className="dropzone-hint">Máx. 5 MB, formato .csv</small>
        </label>
        <input
          type="file"
          id="csvFile"
          accept=".csv"
          className="visually-hidden"
          aria-label="Fichero CSV"
          onChange={e => handleArchivo(e.target.files[0])}
        />
        <div className="import-form-row">
          <label htmlFor="clienteSelect" className="import-label">Cliente (opcional)</label>
          <select
            id="clienteSelect"
            className="import-select"
            value={clienteId}
            onChange={e => setClienteId(e.target.value)}
          >
            <option value="">Sin asignar</option>
            {clientes.map(c => (
              <option key={c.id} value={c.id}>{c.nombre}</option>
            ))}
          </select>
          <button
            type="button"
            className="btn-importar"
            onClick={handleImportar}
            disabled={!archivo || subiendo}
          >
            {subiendo ? 'Importando…' : 'Importar CSV'}
          </button>
        </div>
      </section>

      {loteActivo && (
        <section className="import-card" aria-label="Progreso del lote">
          <h2 className="table-header-title">Lote #{loteActivo.id}</h2>
          <div className="progress-track">
            <div className="progress-bar" style={{ width: `${Math.min(pct, 100)}%` }} />
          </div>
          <p className="dashboard-subtitle">
            {loteActivo.estado} — procesados {loteActivo.procesados} / exitosos {loteActivo.exitosos} / fallidos {loteActivo.fallidos}
            {loteActivo.errorResumen ? ` — ${loteActivo.errorResumen}` : ''}
          </p>
        </section>
      )}

      <section className="table-section">
        <div className="table-header">
          <h2>Lotes recientes</h2>
          <span className="table-count">{lotes.length} lote{lotes.length !== 1 ? 's' : ''}</span>
        </div>

        {lotes.length === 0 ? (
          <EmptyState message="No hay lotes de importación todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>ID</th><th>Archivo</th><th>Cliente</th><th>Estado</th>
                <th>Procesados</th><th>Exitosos</th><th>Fallidos</th><th>Fecha</th><th></th>
              </tr>
            </thead>
            <tbody>
              {lotes.map(l => (
                <tr key={l.id}>
                  <td className="cell-code">{l.id}</td>
                  <td>{l.nombreArchivo}</td>
                  <td>{nombreCliente(l.clienteId)}</td>
                  <td><span className={ESTADO_CLASES[l.estado] || 'lote-badge'}>{l.estado}</span></td>
                  <td>{l.procesados}</td>
                  <td>{l.exitosos}</td>
                  <td>{l.fallidos}</td>
                  <td className="cell-date">
                    {l.fechaCreacion
                      ? new Date(l.fechaCreacion).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })
                      : '-'}
                  </td>
                  <td>
                    <button type="button" className="btn-importar btn-importar--small" onClick={() => handleVerErrores(l.id)}>
                      Errores
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {modalAbierto && (
        <div className="import-modal" role="dialog" aria-modal="true" aria-label="Errores del lote">
          <div className="import-modal-content">
            <div className="import-modal-header">
              <h3>Errores del lote</h3>
              <button type="button" className="import-modal-close" aria-label="Cerrar" onClick={() => setModalAbierto(false)}>×</button>
            </div>
            {errores.length === 0 ? (
              <EmptyState message="No hay errores registrados" />
            ) : (
              <table className="envios-table">
                <thead>
                  <tr><th>Línea</th><th>Código</th><th>Error</th></tr>
                </thead>
                <tbody>
                  {errores.map((e, i) => (
                    <tr key={i}>
                      <td>{e.lineaNumero}</td>
                      <td className="cell-code">{e.codigoRastreo || '-'}</td>
                      <td>{e.errorMensaje}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
