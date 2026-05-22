import { useState, useEffect, useRef, useCallback } from 'react';

export default function usePolling(callback, intervalMs, enabled = true) {
  const [polling, setPolling] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [refreshError, setRefreshError] = useState(null);
  const callbackRef = useRef(callback);
  const pollingRef = useRef(false);
  const intervalRef = useRef(null);

  callbackRef.current = callback;

  const refreshNow = useCallback(async () => {
    if (pollingRef.current) return;
    pollingRef.current = true;
    setPolling(true);
    setRefreshError(null);
    try {
      await callbackRef.current();
      setLastUpdated(Date.now());
    } catch (err) {
      setRefreshError(err instanceof Error ? err.message : 'Error al actualizar');
    } finally {
      pollingRef.current = false;
      setPolling(false);
    }
  }, []);

  useEffect(() => {
    if (!enabled) return;

    const handleVisibility = () => {
      if (document.hidden) {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
      } else {
        refreshNow();
        intervalRef.current = setInterval(refreshNow, intervalMs);
      }
    };

    document.addEventListener('visibilitychange', handleVisibility);
    refreshNow();
    intervalRef.current = setInterval(refreshNow, intervalMs);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibility);
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [intervalMs, enabled, refreshNow]);

  return { polling, lastUpdated, refreshNow, refreshError };
}
