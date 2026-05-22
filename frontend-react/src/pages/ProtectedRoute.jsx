import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="loading">Verificando sesión…</div>;
  if (!user) return <Navigate to="/login-react" state={{ from: location }} replace />;
  return children;
}
