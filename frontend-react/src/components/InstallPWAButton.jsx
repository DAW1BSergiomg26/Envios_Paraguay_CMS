import { useState } from 'react';
import usePWAInstall from '../hooks/usePWAInstall';
import { useToast } from '../context/NotificationContext';

export default function InstallPWAButton() {
  const { canInstall, isInstalled, installApp } = usePWAInstall();
  const { showSuccess, showInfo } = useToast();
  const [installing, setInstalling] = useState(false);

  if (!canInstall || isInstalled) return null;

  const handleInstall = async () => {
    setInstalling(true);
    try {
      const accepted = await installApp();
      if (accepted) {
        showSuccess('App instalada correctamente');
      }
    } catch {
      showInfo('La app ya está instalada');
    } finally {
      setInstalling(false);
    }
  };

  return (
    <button className="btn-install-pwa" onClick={handleInstall} disabled={installing} title="Instalar aplicación">
      {installing ? (
        <span className="btn-install-pwa__spinner" />
      ) : (
        <svg className="btn-install-pwa__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="7 10 12 15 17 10" />
          <line x1="12" y1="15" x2="12" y2="3" />
        </svg>
      )}
      <span>Instalar app</span>
    </button>
  );
}
