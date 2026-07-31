import http from 'k6/http';
import { check } from 'k6';

// Flujo de login admin: GET /login -> extraer _csrf -> POST /login con credenciales.
// k6 mantiene las cookies JSESSIONID por VU en su cookie jar, por lo que una única
// llamada por VU establece la sesión para iteraciones posteriores.
export function adminLogin(baseURL, username, password) {
  const loginPage = http.get(`${baseURL}/login`);
  check(loginPage, {
    'login page GET 200': (r) => r.status === 200,
  });

  const match = loginPage.body.match(/<input[^>]*name="_csrf"[^>]*value="([^"]+)"/);
  check(null, {
    'csrf token present': () => match !== null,
  });
  const csrfToken = match ? match[1] : '';

  const res = http.post(`${baseURL}/login`, {
    username,
    password,
    _csrf: csrfToken,
  });
  check(res, {
    'login POST redirects to dashboard': (r) => r.status === 200 && r.url.includes('/admin/dashboard'),
  });
  return res;
}
