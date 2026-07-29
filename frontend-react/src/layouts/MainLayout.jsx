import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { useOfflineSync } from '../hooks/useOfflineSync';
import InstallPWAButton from '../components/InstallPWAButton';
import PushNotificationButton from '../components/PushNotificationButton';
import OfflineBanner from '../components/OfflineBanner';

export default function MainLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const isOnline = useOnlineStatus();
  
  useOfflineSync();

  const handleLogout = async () => {
    await logout();
    navigate('/login-react', { replace: true });
  };

  return (
    <div className="app-layout">
      <OfflineBanner isOnline={isOnline} />
      {user && (
        <nav className="navbar">
          <span className="navbar-brand" onClick={() => navigate('/')}>
            Monteastur
          </span>
          <div className="navbar-right">
            <InstallPWAButton />
            <PushNotificationButton />
            <span className="navbar-user">{user.username}</span>
            <button className="btn-logout" onClick={handleLogout}>
              Cerrar sesión
            </button>
          </div>
        </nav>
      )}
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
