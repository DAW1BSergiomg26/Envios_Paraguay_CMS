import { createContext, useContext, useState, useCallback, useRef, useEffect } from 'react';

const ToastContext = createContext(null);

let toastId = 0;

const ICONS = {
  success: '✓',
  error: '✕',
  warning: '!',
  info: 'i'
};

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const recentSet = useRef(new Set());
  const timersRef = useRef({});

  const addToast = useCallback((message, type = 'info', duration = 4000) => {
    const key = `${type}:${message}`;
    if (recentSet.current.has(key)) return;
    recentSet.current.add(key);
    setTimeout(() => recentSet.current.delete(key), 2000);

    const id = ++toastId;
    setToasts(prev => [...prev, { id, message, type, icon: ICONS[type] }]);
    timersRef.current[id] = setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
      delete timersRef.current[id];
    }, duration);
  }, []);

  const removeToast = useCallback((id) => {
    clearTimeout(timersRef.current[id]);
    delete timersRef.current[id];
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  useEffect(() => {
    return () => {
      Object.values(timersRef.current).forEach(clearTimeout);
    };
  }, []);

  const showSuccess = useCallback((msg) => addToast(msg, 'success'), [addToast]);
  const showError = useCallback((msg) => addToast(msg, 'error', 6000), [addToast]);
  const showWarning = useCallback((msg) => addToast(msg, 'warning', 5000), [addToast]);
  const showInfo = useCallback((msg) => addToast(msg, 'info'), [addToast]);

  return (
    <ToastContext.Provider value={{ toasts, showSuccess, showError, showWarning, showInfo, removeToast }}>
      {children}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
