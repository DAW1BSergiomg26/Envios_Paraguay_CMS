import http from 'k6/http';
import { check } from 'k6';
import { adminLogin } from './helpers/auth.js';
import { randomDateRange, randomTrackingCode } from './helpers/data.js';

const SCENARIO = __ENV.SCENARIO || 'load';
const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || '';
const TRACKING_CODES = (__ENV.TRACKING_CODES || 'MT-2026-0001,MT-2026-0002,MT-2026-0003,MT-2026-0004,MT-2026-0005').split(',');

// Solo 5xx y errores de red cuentan como request fallido; 4xx esperados (409) no.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

const SCENARIO_OPTIONS = {
  smoke: {
    executor: 'per-vu-iterations',
    vus: 1,
    iterations: 10,
  },
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 50 },
      { duration: '8m', target: 50 },
      { duration: '1m', target: 0 },
    ],
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 10,
    stages: [
      { duration: '2m', target: 100 },
      { duration: '1m', target: 200 },
      { duration: '1m', target: 200 },
      { duration: '1m', target: 0 },
    ],
  },
};

const THRESHOLDS = {
  smoke:  { http_req_duration: ['p(95)<500'],  http_req_failed: ['rate<0.01'] },
  load:   { http_req_duration: ['p(95)<500'],  http_req_failed: ['rate<0.01'] },
  stress: { http_req_duration: ['p(95)<1000'], http_req_failed: ['rate<0.05'] },
};

export const options = {
  scenarios: {
    [SCENARIO]: SCENARIO_OPTIONS[SCENARIO],
  },
  thresholds: THRESHOLDS[SCENARIO],
};

let adminReady = false;

function getTracking() {
  const res = http.get(`${BASE_URL}/api/v1/tracking/${randomTrackingCode(TRACKING_CODES)}`);
  check(res, {
    'tracking GET 200': (r) => r.status === 200,
    'tracking returns codigoUnico': (r) => r.json('codigoUnico') !== undefined,
  });
}

function getDisponibilidad() {
  const { fechaEntrada, fechaSalida } = randomDateRange();
  const res = http.get(`${BASE_URL}/api/v1/reservas/disponibilidad?fechaEntrada=${fechaEntrada}&fechaSalida=${fechaSalida}`);
  check(res, {
    'disponibilidad GET 200': (r) => r.status === 200,
    'disponibilidad returns disponible boolean': (r) => typeof r.json('disponible') === 'boolean',
  });
}

function postReserva() {
  const { fechaEntrada, fechaSalida } = randomDateRange();
  const payload = JSON.stringify({
    nombreCliente: 'Cliente k6',
    email: 'cliente.k6@test.local',
    telefono: '555-0000',
    fechaEntrada,
    fechaSalida,
    numeroHuespedes: 2,
    comentarios: 'Reserva de prueba generada por k6',
  });
  const res = http.post(`${BASE_URL}/api/v1/reservas`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, {
    'reserva POST 201 o 409 (fechas ocupadas)': (r) => r.status === 201 || r.status === 409,
  });
}

function adminFlow() {
  if (!adminReady) {
    const login = adminLogin(BASE_URL, ADMIN_USERNAME, ADMIN_PASSWORD);
    adminReady = login.status === 200;
  }
  if (adminReady) {
    const res = http.get(`${BASE_URL}/api/v1/admin/envios`);
    check(res, {
      'admin envios GET 200': (r) => r.status === 200,
    });
  }
}

export default function () {
  if (SCENARIO === 'smoke') {
    getTracking();
    getDisponibilidad();
    postReserva();
    adminFlow();
    return;
  }
  const roll = Math.random();
  if (roll < 0.60) getTracking();
  else if (roll < 0.85) getDisponibilidad();
  else if (roll < 0.95) postReserva();
  else adminFlow();
}
