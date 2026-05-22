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
  return api.get('/admin/envios', { params });
}

export default api;
