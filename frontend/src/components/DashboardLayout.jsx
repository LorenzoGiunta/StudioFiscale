import { useState, useEffect } from 'react';
import Sidebar from './Sidebar.jsx';
import Header from './Header.jsx';
import { Outlet, useNavigate } from 'react-router-dom';
import notificaService from '../services/NotificaService.js';

/**
 * Struttura di pagina dell'area autenticata: dispone barra laterale e barra
 * superiore attorno al contenuto della rotta corrente e mantiene aggiornato il
 * contatore delle notifiche non lette mostrato nell'intestazione.
 */
export default function DashboardLayout() {
  const navigate = useNavigate();
  const [notifCount, setNotifCount] = useState(0);

  useEffect(() => {
    notificaService.countNonLette()
      .then(n => setNotifCount(n))
      .catch(() => setNotifCount(0));
  }, []);

  return (
    <div className="min-h-screen bg-anthracite-50">
      <Sidebar />
      <div className="ml-[260px] transition-all duration-300">
        <Header
          notificationCount={notifCount}
          onNotificationClick={() => {
            // Navigate to notifications page based on current route
            const path = window.location.pathname;
            if (path.includes('cliente')) navigate('/dashboard/cliente/notifiche');
            else if (path.includes('commercialista')) navigate('/dashboard/commercialista/notifiche');
            else if (path.includes('collaboratore')) navigate('/dashboard/collaboratore/notifiche');
            else if (path.includes('admin')) navigate('/dashboard/admin/notifiche');
          }}
        />
        <main className="p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
