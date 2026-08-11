import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  withCredentials: true
});

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      const data = error.response.data;
      if (typeof data === 'string' && (data.startsWith('<!') || data.includes('login'))) {
        return Promise.reject(new Error('Necesitas iniciar sesión como admin en Spring Boot'));
      }
      if (error.response.status === 403) {
        return Promise.reject(new Error('Acceso denegado. Inicia sesión como admin en Spring Boot'));
      }
    }
    return Promise.reject(error);
  }
);

export function getAdminEnvios(params = {}) {
  return api.get('/admin/envios', { params, paramsSerializer: { indexes: null } });
}

export function getAdminEnvioDetalle(codigo) {
  return api.get(`/admin/envios/${codigo}`);
}

export function putAdminEnvioEstado(codigo, estado) {
  return api.put(`/admin/envios/${codigo}/estado`, { estado });
}

export function getAdminImports() {
  return api.get('/admin/imports');
}

export function getAdminImporte(id) {
  return api.get(`/admin/imports/${id}`);
}

export function getImportErrores(id) {
  return api.get(`/admin/imports/${id}/errors`);
}

export function getAdminClientes() {
  return api.get('/admin/clientes');
}

export function uploadImportCsv(file, clienteId) {
  const formData = new FormData();
  formData.append('file', file);
  if (clienteId) formData.append('clienteId', clienteId);
  return api.post('/admin/imports/csv', formData);
}

export async function getCsrfToken() {
  const res = await fetch('/login', { credentials: 'include' });
  const html = await res.text();
  const match = html.match(/name="_csrf".*?value="([^"]+)"/);
  return match ? match[1] : null;
}

export async function loginUser(username, password) {
  const token = await getCsrfToken();
  if (!token) return false;
  const body = new URLSearchParams({ username, password, _csrf: token });
  await fetch('/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
    credentials: 'include'
  });
  return checkSession();
}

export async function logoutUser() {
  const token = await getCsrfToken();
  if (token) {
    const body = new URLSearchParams({ _csrf: token });
    await fetch('/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
      credentials: 'include'
    });
  }
}

export async function checkSession() {
  try {
    const res = await api.get('/admin/envios?page=0&size=1');
    // Axios follows 3xx redirects to /login (200 HTML). A valid session
    // returns JSON; an invalid one returns the login page (string HTML).
    return typeof res.data === 'object' && res.data !== null;
  } catch {
    return false;
  }
}

export function getDocumentoUrl(tipo, referencia) {
  if (tipo === 'etiqueta') {
    return `/api/v1/admin/documentos/envios/${referencia}/etiqueta`;
  }
  if (tipo === 'etiquetas-lote') {
    return `/api/v1/admin/documentos/lotes/${referencia}/etiquetas`;
  }
  return `/api/v1/admin/documentos/lotes/${referencia}/manifiesto`;
}

export function descargarDocumento(url) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = '';
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
}

export function getAdminDocumentos(tipo) {
  return api.get('/admin/documentos', { params: { tipo } });
}

export function formatPesoBytes(bytes) {
  if (!bytes || bytes <= 0) return '0.0 KB';
  return `${(bytes / 1024).toFixed(1)} KB`;
}

export function getAdminReservas(estado) {
  const params = {};
  if (estado) params.estado = estado;
  return api.get('/admin/reservas', { params });
}

export function getAdminReservaDetalle(id) {
  return api.get(`/admin/reservas/${id}`);
}

export function putAdminReserva(id, body) {
  return api.put(`/admin/reservas/${id}`, body);
}

export function patchAdminReservaEstado(id, estado) {
  return api.patch(`/admin/reservas/${id}/estado`, { estado });
}

export function deleteAdminReserva(id) {
  return api.delete(`/admin/reservas/${id}`);
}

export function getAdminMensajes(leido) {
  const params = {};
  if (leido !== undefined && leido !== null) params.leido = leido;
  return api.get('/admin/mensajes', { params });
}

export function patchAdminMensajeLeido(id, leido) {
  return api.patch(`/admin/mensajes/${id}/leido`, { leido });
}

export function deleteAdminMensaje(id) {
  return api.delete(`/admin/mensajes/${id}`);
}

export function getAdminImagenes() {
  return api.get('/admin/imagenes');
}

export function uploadAdminImagen(formData) {
  return api.post('/admin/imagenes', formData);
}

export function patchAdminImagenOrden(id, orden) {
  return api.patch(`/admin/imagenes/${id}/orden`, { orden });
}

export function deleteAdminImagen(id) {
  return api.delete(`/admin/imagenes/${id}`);
}

export function getAdminTextos() {
  return api.get('/admin/textos');
}

export function getTextoLegal(slug) {
  return api.get(`/admin/textos/${slug}`);
}

export function putTextoLegal(slug, { titulo, contenido }) {
  return api.put(`/admin/textos/${slug}`, { titulo, contenido });
}

export function postAdminEnvio(data) {
  return api.post('/admin/envios', data);
}

export function putAdminEnvio(codigo, data) {
  return api.put(`/admin/envios/${codigo}`, data);
}

export function deleteAdminEnvio(codigo) {
  return api.delete(`/admin/envios/${codigo}`);
}

export function uploadAdminEvidencia(codigo, formData) {
  return api.post(`/admin/envios/${codigo}/evidencias`, formData);
}

export function patchAdminEvidenciaVisibilidad(id, visibleCliente) {
  return api.patch(`/admin/envios/evidencias/${id}/visibilidad`, { visibleCliente });
}

export function deleteAdminEvidencia(id) {
  return api.delete(`/admin/envios/evidencias/${id}`);
}

export default api;
