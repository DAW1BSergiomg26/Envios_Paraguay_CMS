import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/NotificationContext';
import ToastContainer from './components/ToastContainer';
import MainLayout from './layouts/MainLayout';
import ProtectedRoute from './pages/ProtectedRoute';
import { SkeletonCard } from './components/SkeletonLoader';

const LoginPage = lazy(() => import('./pages/LoginPage'));
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const ShipmentDetailPage = lazy(() => import('./pages/ShipmentDetailPage'));
const EnvioFormPage = lazy(() => import('./pages/EnvioFormPage'));
const ImportBatchPage = lazy(() => import('./pages/ImportBatchPage'));
const DocumentosPage = lazy(() => import('./pages/DocumentosPage'));
const ReservasPage = lazy(() => import('./pages/ReservasPage'));
const MensajesPage = lazy(() => import('./pages/MensajesPage'));
const AdminImagesPage = lazy(() => import('./pages/AdminImagesPage'));
const AdminLegalTextsPage = lazy(() => import('./pages/AdminLegalTextsPage'));
const WebhooksPage = lazy(() => import('./pages/WebhooksPage'));
const NotificacionesPage = lazy(() => import('./pages/NotificacionesPage'));

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <ToastContainer />
          <Suspense fallback={<div style={{ padding: 24 }}><SkeletonCard /></div>}>
            <Routes>
              <Route path="/login-react" element={<LoginPage />} />
              <Route element={<MainLayout />}>
                <Route path="/" element={<ProtectedRoute><AdminDashboard /></ProtectedRoute>} />
                <Route path="/dashboard/envio/:codigo" element={<ProtectedRoute><ShipmentDetailPage /></ProtectedRoute>} />
                <Route path="/dashboard/envios/nuevo" element={<ProtectedRoute><EnvioFormPage /></ProtectedRoute>} />
                <Route path="/dashboard/envios/:codigo/editar" element={<ProtectedRoute><EnvioFormPage /></ProtectedRoute>} />
                <Route path="/dashboard/imports" element={<ProtectedRoute><ImportBatchPage /></ProtectedRoute>} />
                <Route path="/dashboard/reservas" element={<ProtectedRoute><ReservasPage /></ProtectedRoute>} />
                <Route path="/dashboard/documentos" element={<ProtectedRoute><DocumentosPage /></ProtectedRoute>} />
                <Route path="/dashboard/mensajes" element={<ProtectedRoute><MensajesPage /></ProtectedRoute>} />
                <Route path="/dashboard/imagenes" element={<ProtectedRoute><AdminImagesPage /></ProtectedRoute>} />
                <Route path="/dashboard/textos" element={<ProtectedRoute><AdminLegalTextsPage /></ProtectedRoute>} />
                <Route path="/dashboard/webhooks" element={<ProtectedRoute><WebhooksPage /></ProtectedRoute>} />
                <Route path="/dashboard/notificaciones" element={<ProtectedRoute><NotificacionesPage /></ProtectedRoute>} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
