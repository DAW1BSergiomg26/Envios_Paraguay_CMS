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
    credentials: 'include',
    redirect: 'manual'
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
      credentials: 'include',
      redirect: 'manual'
    });
  }
}

export async function checkSession() {
  try {
    await api.get('/admin/envios?page=0&size=1');
    return true;
  } catch {
    return false;
  }
}

export default api;
