import { usePushNotifications } from '../hooks/usePushNotifications';
import { useToast } from '../context/NotificationContext';
import api from '../services/api';

export default function PushNotificationButton() {
  const { supported, permission, subscribed, loading, subscribe, unsubscribe } = usePushNotifications();
  const { showSuccess, showError, showInfo } = useToast();

  if (!supported) return null;

  const handleToggle = async () => {
    if (loading) return;

    if (permission === 'denied') {
      showError('Notificaciones bloqueadas por el navegador');
      return;
    }

    if (subscribed) {
      await unsubscribe();
      showInfo('Notificaciones desactivadas');
    } else {
      await subscribe();
      if (Notification.permission === 'granted') {
        showSuccess('Notificaciones activadas');
      } else {
        showError('Permiso denegado');
      }
    }
  };

  const handleTest = async () => {
    try {
      await api.post('/push/test');
      showInfo('Notificación de prueba enviada');
    } catch (e) {
      showError('Error al enviar prueba');
    }
  };

  return (
    <div className="push-container">
      <button
        className={`btn-push ${subscribed ? 'btn-push--active' : ''}`}
        onClick={handleToggle}
        disabled={loading}
        title={subscribed ? 'Desactivar notificaciones' : 'Activar notificaciones'}
      >
        {loading ? (
          <span className="btn-push__spinner" />
        ) : (
          <svg className="btn-push__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
        )}
      </button>
      {subscribed && (
        <button className="btn-push-test" onClick={handleTest} title="Notificación de prueba">Test</button>
      )}
    </div>
  );
}
