const QUEUE_KEY = 'MONTEASTUR_OFFLINE_QUEUE';

export const getQueue = () => {
  try {
    const queue = localStorage.getItem(QUEUE_KEY);
    return queue ? JSON.parse(queue) : [];
  } catch (e) {
    console.error('Error parsing queue:', e);
    return [];
  }
};

export const enqueueOperation = (operation) => {
  const queue = getQueue();
  // Evitar duplicados (mismo código y mismo estado)
  const isDuplicate = queue.some(op => op.codigo === operation.codigo && op.estado === operation.estado);
  if (isDuplicate) return;

  queue.push({ ...operation, id: Date.now(), createdAt: new Date().toISOString() });
  localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
};

export const removeFromQueue = (id) => {
  const queue = getQueue().filter(op => op.id !== id);
  localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
};
