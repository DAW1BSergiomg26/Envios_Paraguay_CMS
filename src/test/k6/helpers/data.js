function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function toISODate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// Rango futuro de fechas: inicio +minStartDays..+maxStartDays, salida = inicio + 1..maxSpanDays.
// Garantiza salida > entrada para no provocar 400 en la API.
export function randomDateRange(minStartDays = 30, maxStartDays = 180, maxSpanDays = 20) {
  const now = new Date();
  const start = new Date(now.getTime() + randomInt(minStartDays, maxStartDays) * 86400000);
  const end = new Date(start.getTime() + randomInt(1, maxSpanDays) * 86400000);
  return { fechaEntrada: toISODate(start), fechaSalida: toISODate(end) };
}

export function randomTrackingCode(codes) {
  return codes[randomInt(0, codes.length - 1)];
}
