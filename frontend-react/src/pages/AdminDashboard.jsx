import { useState, useEffect } from 'react';
import { getAdminEnvios } from '../services/api';

export default function AdminDashboard() {
  const [envios, setEnvios] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [apiStatus, setApiStatus] = useState('checking');

  useEffect(() => {
    getAdminEnvios({ page: 0, size: 5 })
      .then(res => {
        setEnvios(res.data.content || []);
        setTotal(res.data.totalElements || 0);
        setApiStatus('connected');
      })
      .catch(err => {
        setError(err.message || 'Error de conexión');
        setApiStatus('error');
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading">Cargando...</div>;

  const needsLogin = error && (
    error.toLowerCase().includes('sesión') ||
    error.toLowerCase().includes('login') ||
    error.toLowerCase().includes('denegado')
  );

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>Monteastur Admin</h1>
        <span className={`status status--${apiStatus}`}>
          API: {apiStatus === 'connected' ? 'Conectado' : 'Error'}
        </span>
      </header>

      {error && needsLogin && (
        <div className="error-banner error-banner--login">
          <strong>Se requiere autenticación</strong>
          <p>{error}</p>
          <ol>
            <li>Abre <a href="http://localhost:8895/login" target="_blank" rel="noreferrer">http://localhost:8895/login</a></li>
            <li>Inicia sesión con admin / admin123</li>
            <li>Vuelve a <a href="/" onClick={e => { e.preventDefault(); window.location.reload(); }}>recargar esta página</a></li>
          </ol>
        </div>
      )}

      {error && !needsLogin && (
        <div className="error-banner">Error: {error}</div>
      )}

      <section className="summary">
        <div className="card">
          <strong>{total}</strong>
          <span>Envíos registrados</span>
        </div>
        <div className="card">
          <strong>{envios.length}</strong>
          <span>Mostrando en página</span>
        </div>
      </section>

      <section className="table-section">
        <h2>Últimos envíos</h2>
        <table className="envios-table">
          <thead>
            <tr>
              <th>Código</th>
              <th>Estado</th>
              <th>Destinatario</th>
              <th>Origen</th>
              <th>Destino</th>
              <th>Última actualización</th>
            </tr>
          </thead>
          <tbody>
            {envios.map(e => (
              <tr key={e.codigoUnico}>
                <td>{e.codigoUnico}</td>
                <td><span className={`badge badge--${e.estado?.toLowerCase()}`}>{e.estado}</span></td>
                <td>{e.destinatario}</td>
                <td>{e.origen}</td>
                <td>{e.destino}</td>
                <td>{new Date(e.ultimaActualizacion).toLocaleDateString()}</td>
              </tr>
            ))}
            {envios.length === 0 && !error && (
              <tr><td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>No hay envíos</td></tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}
