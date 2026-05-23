/// <reference lib="WebWorker" />

// Injected by vite-plugin-pwa
self.__WB_MANIFEST;

self.addEventListener('push', (event) => {
  let data = { title: 'Monteastur Envios', body: 'Notificación recibida' };
  if (event.data) {
    try {
      data = event.data.json();
    } catch {
      data = { title: 'Monteastur Envios', body: event.data.text() };
    }
  }
  const options = {
    body: data.body,
    icon: '/icons/icon-192.svg',
    badge: '/icons/icon-192.svg',
    vibrate: [200, 100, 200],
    data: { url: data.url || '/' },
    requireInteraction: true,
    silent: false
  };
  event.waitUntil(self.registration.showNotification(data.title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const urlToOpen = event.notification.data?.url || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (const client of windowClients) {
        if (client.url.includes(urlToOpen) && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(urlToOpen);
      }
    })
  );
});
