export function parseLocalDateTime(value) {
  if (!value) return new Date();
  if (Array.isArray(value)) {
    return new Date(value[0], value[1] - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0);
  }
  if (typeof value === 'string') {
    const d = new Date(value);
    if (!isNaN(d.getTime())) return d;
  }
  return new Date();
}
