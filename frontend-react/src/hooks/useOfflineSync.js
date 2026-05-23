import { useCallback, useRef } from 'react';
import { getQueue, removeFromQueue } from '../services/offlineQueue';
import { putAdminEnvioEstado } from '../services/api';
import { useToast } from '../context/NotificationContext';

export function useOfflineSync() {
  const { showSuccess, showWarning, showInfo } = useToast();
  const syncing = useRef(false);

  const sync = useCallback(async () => {
    if (syncing.current || !navigator.onLine) return;
    
    const queue = getQueue();
    if (queue.length === 0) return;

    syncing.current = true;
    showInfo(`Sincronizando ${queue.length} cambios pendientes...`);

    let successCount = 0;
    let failCount = 0;

    for (const op of queue) {
      if (op.type === 'UPDATE_ESTADO') {
        try {
          await putAdminEnvioEstado(op.codigo, op.estado);
          removeFromQueue(op.id);
          successCount++;
        } catch (err) {
          console.error('Error al sincronizar:', err);
          failCount++;
        }
      }
    }

    if (successCount > 0) showSuccess(`${successCount} cambios sincronizados`);
    if (failCount > 0) showWarning(`${failCount} cambios fallaron, se reintentará luego`);
    
    syncing.current = false;
  }, [showSuccess, showWarning, showInfo]);

  useEffect(() => {
    window.addEventListener('online', sync);
    // Intentar sync al cargar si hay conexión
    if (navigator.onLine) sync();
    return () => window.removeEventListener('online', sync);
  }, [sync]);
}
