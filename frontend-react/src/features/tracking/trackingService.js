import { getAdminEnvios, deleteAdminEnvio } from '../../services/api';
import { DEFAULT_PAGE_SIZE } from './trackingConstants';

export function buildTrackingParams({
  page = 0,
  size = DEFAULT_PAGE_SIZE,
  estados = [],
  query = '',
  fechaDesde = '',
  fechaHasta = '',
  sort = '',
} = {}) {
  const params = { page, size };
  if (estados && estados.length > 0) params.estados = estados;
  if (query) params.q = query;
  if (fechaDesde) params.fechaDesde = fechaDesde;
  if (fechaHasta) params.fechaHasta = fechaHasta;
  if (sort) params.sort = sort;
  return params;
}

export function fetchEnvios(filters) {
  return getAdminEnvios(buildTrackingParams(filters));
}

export function deleteEnvio(codigo) {
  return deleteAdminEnvio(codigo);
}
