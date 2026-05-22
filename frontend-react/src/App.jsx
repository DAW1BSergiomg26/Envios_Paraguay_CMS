import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import AdminDashboard from './pages/AdminDashboard';
import ShipmentDetailPage from './pages/ShipmentDetailPage';
import LoginPage from './pages/LoginPage';
import ProtectedRoute from './pages/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login-react" element={<LoginPage />} />
          <Route element={<MainLayout />}>
            <Route path="/" element={
              <ProtectedRoute><AdminDashboard /></ProtectedRoute>
            } />
            <Route path="/dashboard/envio/:codigo" element={
              <ProtectedRoute><ShipmentDetailPage /></ProtectedRoute>
            } />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
