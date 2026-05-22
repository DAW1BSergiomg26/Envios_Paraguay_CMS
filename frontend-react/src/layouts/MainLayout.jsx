import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import InstallPWAButton from '../components/InstallPWAButton';

export default function MainLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login-react', { replace: true });
  };

  return (
    <div className="app-layout">
      {user && (
        <nav className="navbar">
          <span className="navbar-brand" onClick={() => navigate('/')}>
            Monteastur
          </span>
          <div className="navbar-right">
            <InstallPWAButton />
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
