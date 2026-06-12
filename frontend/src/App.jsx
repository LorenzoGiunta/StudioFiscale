import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext.jsx';
import { ToastProvider } from './contexts/ToastContext.jsx';

import LoginPage from './pages/auth/LoginPage.jsx';
import RegisterPage from './pages/auth/RegisterPage.jsx';
import DashboardLayout from './components/DashboardLayout.jsx';

// Cliente pages
import ClienteDashboard from './pages/cliente/ClienteDashboard.jsx';
import ClientePratiche from './pages/cliente/ClientePratiche.jsx';
import ClienteDocumenti from './pages/cliente/ClienteDocumenti.jsx';
import ClienteProfilo from './pages/cliente/ClienteProfilo.jsx';

// Commercialista pages
import CommercialistaDashboard from './pages/commercialista/CommercialistaDashboard.jsx';
import CommercialistaPratiche from './pages/commercialista/CommercialistaPratiche.jsx';
import CommercialistaDocumenti from './pages/commercialista/CommercialistaDocumenti.jsx';
import CommercialistaCollaboratori from './pages/commercialista/CommercialistaCollaboratori.jsx';
import CommercialistaImposte from './pages/commercialista/CommercialistaImposte.jsx';
import CommercialistaClienti from './pages/commercialista/CommercialistaClienti.jsx';
import CommercialistaClienteDettaglio from './pages/commercialista/CommercialistaClienteDettaglio.jsx';

// Collaboratore pages
import CollaboratoreDashboard from './pages/collaboratore/CollaboratoreDashboard.jsx';
import CollaboratorePratiche from './pages/collaboratore/CollaboratorePratiche.jsx';
import CollaboratoreDocumenti from './pages/collaboratore/CollaboratoreDocumenti.jsx';
import CollaboratoreMieiStudi from './pages/collaboratore/CollaboratoreMieiStudi.jsx';

// Admin pages
import AdminDashboard from './pages/admin/AdminDashboard.jsx';
import AdminUtenti from './pages/admin/AdminUtenti.jsx';

// Shared
import ChatView from './components/ChatView.jsx';
import ApiStatus from './components/ApiStatus.jsx';
import NotificheView from './components/NotificheView.jsx';
import InvitoAccettazionePage from './pages/InvitoAccettazionePage.jsx';
import PraticaDetail from './pages/shared/PraticaDetail.jsx';

/**
 * Wrapper di rotta che ne consente l'accesso solo agli utenti autenticati e, se
 * indicato, ai soli ruoli ammessi; negli altri casi reindirizza al login.
 */
function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(user?.ruolo)) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function AppRoutes() {
  const { isAuthenticated, getDashboardRoute } = useAuth();

  return (
    <Routes>
      {/* Auth */}
      <Route path="/login" element={isAuthenticated ? <Navigate to={getDashboardRoute()} replace /> : <LoginPage />} />
      <Route path="/register" element={isAuthenticated ? <Navigate to={getDashboardRoute()} replace /> : <RegisterPage />} />

      {/* Test connessione API */}
      <Route path="/api-status" element={<ApiStatus />} />

      {/* Cliente */}
      <Route path="/dashboard/cliente" element={
        <ProtectedRoute allowedRoles={['CLIENTE']}>
          <DashboardLayout />
        </ProtectedRoute>
      }>
        <Route index element={<ClienteDashboard />} />
        <Route path="pratiche" element={<ClientePratiche />} />
        <Route path="pratiche/:id" element={<PraticaDetail />} />
        <Route path="documenti" element={<ClienteDocumenti />} />
        <Route path="profilo" element={<ClienteProfilo />} />
        <Route path="notifiche" element={<NotificheView />} />
        <Route path="chat" element={<ChatView />} />
      </Route>

      {/* Commercialista */}
      <Route path="/dashboard/commercialista" element={
        <ProtectedRoute allowedRoles={['COMMERCIALISTA']}>
          <DashboardLayout />
        </ProtectedRoute>
      }>
        <Route index element={<CommercialistaDashboard />} />
        <Route path="pratiche" element={<CommercialistaPratiche />} />
        <Route path="pratiche/:id" element={<PraticaDetail />} />
        <Route path="clienti" element={<CommercialistaClienti />} />
        <Route path="clienti/:id" element={<CommercialistaClienteDettaglio />} />
        <Route path="documenti" element={<CommercialistaDocumenti />} />
        <Route path="collaboratori" element={<CommercialistaCollaboratori />} />
        <Route path="imposte" element={<CommercialistaImposte />} />
        <Route path="notifiche" element={<NotificheView />} />
        <Route path="chat" element={<ChatView />} />
      </Route>

      {/* Collaboratore */}
      <Route path="/dashboard/collaboratore" element={
        <ProtectedRoute allowedRoles={['COLLABORATORE']}>
          <DashboardLayout />
        </ProtectedRoute>
      }>
        <Route index element={<CollaboratoreDashboard />} />
        <Route path="pratiche" element={<CollaboratorePratiche />} />
        <Route path="pratiche/:id" element={<PraticaDetail />} />
        <Route path="documenti" element={<CollaboratoreDocumenti />} />
        <Route path="studi" element={<CollaboratoreMieiStudi />} />
        <Route path="notifiche" element={<NotificheView />} />
        <Route path="chat" element={<ChatView />} />
      </Route>

      {/* Admin */}
      <Route path="/dashboard/admin" element={
        <ProtectedRoute allowedRoles={['AMMINISTRATORE']}>
          <DashboardLayout />
        </ProtectedRoute>
      }>
        <Route index element={<AdminDashboard />} />
        <Route path="utenti" element={<AdminUtenti />} />
        <Route path="notifiche" element={<NotificheView />} />
      </Route>

      {/* Pagina pubblica per link email invito (accetta / rifiuta) */}
      <Route path="/invito/:token/:azione" element={<InvitoAccettazionePage />} />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

/**
 * Radice dell'applicazione: avvolge l'albero nei provider di autenticazione e
 * notifiche e definisce il routing, organizzato per ruolo con rotte protette e
 * le pagine pubbliche di accesso e di gestione degli inviti.
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <AppRoutes />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
