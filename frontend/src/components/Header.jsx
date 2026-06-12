import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext.jsx';
import { Bell, ChevronDown, LogOut, User } from 'lucide-react';

/**
 * Barra superiore dell'area autenticata.
 *
 * Mostra l'indicatore delle notifiche non lette e il menu utente con i dati del
 * profilo e l'uscita. Il menu a discesa si chiude al clic esterno.
 */
export default function Header({ notificationCount = 0, onNotificationClick }) {
  const { user, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClick(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const roleLabel = {
    CLIENTE: 'Cliente',
    COMMERCIALISTA: 'Commercialista',
    COLLABORATORE: 'Collaboratore',
    AMMINISTRATORE: 'Amministratore',
  };

  return (
    <header className="h-16 bg-white border-b border-anthracite-100 flex items-center justify-between px-6 sticky top-0 z-30">
      <div>
        <h2 className="text-lg font-semibold text-navy-900 leading-none">
          {/* Page title injected by pages */}
        </h2>
      </div>

      <div className="flex items-center gap-4">
        {/* Notifications */}
        <button
          onClick={onNotificationClick}
          className="relative p-2 text-anthracite-500 hover:text-navy-900 hover:bg-anthracite-50 rounded-sm transition-colors"
          aria-label="Notifiche"
        >
          <Bell size={19} strokeWidth={1.75} />
          {notificationCount > 0 && (
            <span className="absolute top-1 right-1 w-4 h-4 bg-amber-500 text-[10px] font-bold text-white rounded-full flex items-center justify-center">
              {notificationCount > 9 ? '9+' : notificationCount}
            </span>
          )}
        </button>

        {/* User dropdown */}
        <div ref={dropdownRef} className="relative">
          <button
            onClick={() => setShowDropdown(!showDropdown)}
            className="flex items-center gap-2.5 px-3 py-2 hover:bg-anthracite-50 rounded-sm transition-colors"
          >
            <div className="w-8 h-8 rounded-sm bg-navy-900 flex items-center justify-center text-xs font-semibold text-amber-400">
              {user?.nome?.[0]}{user?.cognome?.[0]}
            </div>
            <div className="text-left hidden sm:block">
              <p className="text-sm font-medium text-navy-900 leading-tight">
                {user?.nome} {user?.cognome}
              </p>
              <p className="text-[11px] text-anthracite-400 uppercase tracking-wider font-medium">
                {roleLabel[user?.ruolo]}
              </p>
            </div>
            <ChevronDown size={14} className="text-anthracite-400" />
          </button>

          {showDropdown && (
            <div className="absolute right-0 top-full mt-1 w-52 bg-white border border-anthracite-100 shadow-lg rounded-sm py-1 animate-fade-in">
              <div className="px-4 py-3 border-b border-anthracite-100">
                <p className="text-sm font-medium text-navy-900">
                  {user?.nome} {user?.cognome}
                </p>
                <p className="text-xs text-anthracite-400 mt-0.5">{user?.email}</p>
              </div>
              <button
                onClick={logout}
                className="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-anthracite-600 hover:bg-anthracite-50 hover:text-red-600 transition-colors"
              >
                <LogOut size={15} strokeWidth={1.75} />
                Esci
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
