const DASHBOARD_KEY = 'MONTEASTUR_DASHBOARD';
const DETAIL_KEY_PREFIX = 'MONTEASTUR_DETAIL_';

export const saveDashboardCache = (data) => {
  localStorage.setItem(DASHBOARD_KEY, JSON.stringify({ data, timestamp: Date.now() }));
};

export const getDashboardCache = () => {
  const cached = localStorage.getItem(DASHBOARD_KEY);
  return cached ? JSON.parse(cached) : null;
};

export const saveDetailCache = (codigo, data) => {
  localStorage.setItem(`${DETAIL_KEY_PREFIX}${codigo}`, JSON.stringify({ data, timestamp: Date.now() }));
};

export const getDetailCache = (codigo) => {
  const cached = localStorage.getItem(`${DETAIL_KEY_PREFIX}${codigo}`);
  return cached ? JSON.parse(cached) : null;
};
