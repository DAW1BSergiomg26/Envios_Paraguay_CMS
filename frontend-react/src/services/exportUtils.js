import * as XLSX from 'xlsx';

const CSV_COLUMNS = [
  { header: 'Código', key: 'codigoUnico' },
  { header: 'Estado', key: 'estado' },
  { header: 'Destinatario', key: 'destinatario' },
  { header: 'Origen', key: 'origen' },
  { header: 'Destino', key: 'destino' },
  { header: 'Fecha actualización', key: 'ultimaActualizacion' },
];

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('es-ES', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
}

function formatEstado(estado) {
  if (!estado) return '';
  return estado.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
}

function mapRow(e) {
  return {
    codigoUnico: e.codigoUnico || '',
    estado: formatEstado(e.estado),
    destinatario: e.destinatario || '',
    origen: e.origen || '',
    destino: e.destino || '',
    ultimaActualizacion: formatDate(e.ultimaActualizacion),
  };
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

export function exportToCSV(envios, filenamePrefix = 'envios-dashboard') {
  const rows = envios.map(mapRow);
  const header = CSV_COLUMNS.map(c => c.header).join(',');
  const csvRows = rows.map(r =>
    CSV_COLUMNS.map(c => {
      const val = r[c.key];
      const escaped = String(val).replace(/"/g, '""');
      return `"${escaped}"`;
    }).join(',')
  );
  const csv = '\uFEFF' + [header, ...csvRows].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const now = new Date().toISOString().slice(0, 10);
  downloadBlob(blob, `${filenamePrefix}-${now}.csv`);
}

export function exportToExcel(envios, filenamePrefix = 'envios-dashboard') {
  const rows = envios.map(e => {
    const r = mapRow(e);
    const obj = {};
    CSV_COLUMNS.forEach(c => { obj[c.header] = r[c.key]; });
    return obj;
  });

  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.json_to_sheet(rows);

  const colWidths = CSV_COLUMNS.map(c => ({ wch: Math.max(c.header.length, 16) }));
  ws['!cols'] = colWidths;

  XLSX.utils.book_append_sheet(wb, ws, 'Envíos');
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
  const blob = new Blob([wbout], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  const now = new Date().toISOString().slice(0, 10);
  downloadBlob(blob, `${filenamePrefix}-${now}.xlsx`);
}
