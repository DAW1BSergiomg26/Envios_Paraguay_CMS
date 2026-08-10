import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/NotificationContext';
import ToastContainer from './components/ToastContainer';
import MainLayout from './layouts/MainLayout';
import AdminDashboard from './pages/AdminDashboard';
import ShipmentDetailPage from './pages/ShipmentDetailPage';
import ImportBatchPage from './pages/ImportBatchPage';
import DocumentosPage from './pages/DocumentosPage';
import ReservasPage from './pages/ReservasPage';
import MensajesPage from './pages/MensajesPage';
import LoginPage from './pages/LoginPage';
import ProtectedRoute from './pages/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <ToastContainer />
          <Routes>
          <Route path="/login-react" element={<LoginPage />} />
          <Route element={<MainLayout />}>
            <Route path="/" element={
              <ProtectedRoute><AdminDashboard /></ProtectedRoute>
            } />
            <Route path="/dashboard/envio/:codigo" element={
              <ProtectedRoute><ShipmentDetailPage /></ProtectedRoute>
            } />
            <Route path="/dashboard/imports" element={
              <ProtectedRoute><ImportBatchPage /></ProtectedRoute>
            } />
            <Route path="/dashboard/reservas" element={
              <ProtectedRoute><ReservasPage /></ProtectedRoute>
            } />
            <Route path="/dashboard/documentos" element={
              <ProtectedRoute><DocumentosPage /></ProtectedRoute>
            } />
            <Route path="/dashboard/mensajes" element={
              <ProtectedRoute><MensajesPage /></ProtectedRoute>
            } />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
