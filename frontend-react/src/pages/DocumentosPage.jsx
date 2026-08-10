import { useState, useEffect, useCallback } from 'react';
import { getAdminDocumentos, formatPesoBytes } from '../services/api';
import { parseLocalDateTime } from '../services/dateUtils';
import { useToast } from '../context/NotificationContext';
import EmptyState from '../components/EmptyState';

const TIPOS = [
  { value: '', label: 'Todos los tipos' },
  { value: 'ETIQUETA_TERMICA', label: 'Etiqueta térmica' },
  { value: 'ETIQUETAS_LOTE', label: 'Etiquetas de lote' },
  { value: 'MANIFIESTO_CARGA', label: 'Manifiesto de carga' },
];

const TIPO_BADGE = {
  ETIQUETA_TERMICA: 'lote-badge lote-badge--info',
  ETIQUETAS_LOTE: 'lote-badge lote-badge--warning',
  MANIFIESTO_CARGA: 'lote-badge lote-badge--success',
};

export default function DocumentosPage() {
  const { showError } = useToast();
  const [emisiones, setEmisiones] = useState([]);
  const [tipo, setTipo] = useState('');
  const [loading, setLoading] = useState(true);

  const cargar = useCallback(async (tipoFiltro) => {
    try {
      const res = await getAdminDocumentos(tipoFiltro || undefined);
      setEmisiones(res.data || []);
    } catch (err) {
      showError(err.message || 'Error al cargar las emisiones');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    setLoading(true);
    cargar(tipo);
  }, [tipo, cargar]);

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Auditoría de documentos</h1>
          <p className="dashboard-subtitle">Emisiones de PDFs generados por el equipo.</p>
        </div>
        <label className="import-label" htmlFor="tipoFiltro">Filtrar por tipo</label>
      </header>

      <div className="import-form-row">
        <select
          id="tipoFiltro"
          className="import-select"
          value={tipo}
          onChange={e => setTipo(e.target.value)}
          aria-label="Filtrar por tipo"
        >
          {TIPOS.map(t => (
            <option key={t.value} value={t.value}>{t.label}</option>
          ))}
        </select>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Emisiones</h2>
          <span className="table-count">{emisiones.length} documento{emisiones.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando emisiones…" />
        ) : emisiones.length === 0 ? (
          <EmptyState message="No hay documentos generados todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Tipo</th><th>Referencia</th><th>Archivo</th>
                <th>Peso</th><th>Usuario</th><th>Fecha</th>
              </tr>
            </thead>
            <tbody>
              {emisiones.map(e => (
                <tr key={e.id}>
                  <td><span className={TIPO_BADGE[e.tipo] || 'lote-badge'}>{e.tipo}</span></td>
                  <td className="cell-code">{e.referenciaId}</td>
                  <td>{e.nombreArchivo}</td>
                  <td className="cell-date">{formatPesoBytes(e.pesoBytes)}</td>
                  <td>{e.usuarioGeneracion}</td>
                  <td className="cell-date">
                    {e.fechaCreacion
                      ? parseLocalDateTime(e.fechaCreacion).toLocaleString('es-ES', {
                          day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                        })
                      : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
