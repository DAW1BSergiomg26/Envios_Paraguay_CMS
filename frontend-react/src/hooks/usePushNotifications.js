import { useState, useEffect } from 'react';
import api from '../services/api';

const PUBLIC_VAPID_KEY = 'BEl62iL5b6Vp73H-Wp2s-T1B5g4k9tM7L8f3q0k1p9m8n7v6w5u4t3s2r1q0p9o8n7m6l5k4j3i2h1g';

export function usePushNotifications() {
  const [supported, setSupported] = useState(false);
  const [permission, setPermission] = useState('default');
  const [subscribed, setSubscribed] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if ('Notification' in window && 'serviceWorker' in navigator) {
      setSupported(true);
      setPermission(Notification.permission);
      checkSubscription();
    }
  }, []);

  const checkSubscription = async () => {
    try {
      const registration = await navigator.serviceWorker.ready;
      const sub = await registration.pushManager.getSubscription();
      setSubscribed(!!sub);
    } catch (e) {
      console.error('Check sub error:', e);
    }
  };

  const subscribe = async () => {
    setLoading(true);
    try {
      const permissionResult = await Notification.requestPermission();
      setPermission(permissionResult);
      if (permissionResult !== 'granted') return;

      const registration = await navigator.serviceWorker.ready;
      const sub = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: PUBLIC_VAPID_KEY
      });
      await api.post('/push/subscribe', sub);
      setSubscribed(true);
    } catch (err) {
      console.error('Push subscribe error:', err);
    } finally {
      setLoading(false);
    }
  };

  const unsubscribe = async () => {
    setLoading(true);
    try {
      const registration = await navigator.serviceWorker.ready;
      const sub = await registration.pushManager.getSubscription();
      if (sub) {
        await api.post('/push/unsubscribe', sub);
        await sub.unsubscribe();
        setSubscribed(false);
      }
    } catch (err) {
      console.error('Push unsubscribe error:', err);
    } finally {
      setLoading(false);
    }
  };

  return { supported, permission, subscribed, loading, subscribe, unsubscribe };
}
