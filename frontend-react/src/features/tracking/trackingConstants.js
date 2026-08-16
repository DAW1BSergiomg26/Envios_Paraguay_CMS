export const ESTADOS = [
  { value: 'RECIBIDO', label: 'Recibido' },
  { value: 'EN_ADUANA_ORIGEN', label: 'Aduana Origen' },
  { value: 'EN_TRANSITO', label: 'En Tránsito' },
  { value: 'EN_ADUANA_DESTINO', label: 'Aduana Destino' },
  { value: 'EN_REPARTO', label: 'En Reparto' },
  { value: 'ENTREGADO', label: 'Entregado' },
];

export const ESTADO_LABELS = ESTADOS.reduce((acc, e) => {
  acc[e.value] = e.label;
  return acc;
}, {});

export const DEFAULT_PAGE_SIZE = 10;
export const PAGE_SIZE_OPTIONS = [10, 25, 50];
