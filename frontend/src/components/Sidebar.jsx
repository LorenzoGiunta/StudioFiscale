import { useState, useEffect, useRef } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext.jsx';
import {
  LayoutDashboard,
  FileText,
  Upload,
  Bell,
  MessageSquare,
  Users,
  FolderOpen,
  ClipboardCheck,
  ChevronLeft,
  ChevronRight,
  LogOut,
  Shield,
  UserCheck,
  Building2,
  Calculator,
  User,
} from 'lucide-react';

const NAV_ITEMS = {
  CLIENTE: [
    { to: '/dashboard/cliente', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/dashboard/cliente/pratiche', icon: FileText, label: 'Pratiche' },
    { to: '/dashboard/cliente/documenti', icon: Upload, label: 'Documenti' },
    { to: '/dashboard/cliente/profilo', icon: User, label: 'Profilo' },
    { to: '/dashboard/cliente/notifiche', icon: Bell, label: 'Notifiche' },
    { to: '/dashboard/cliente/chat', icon: MessageSquare, label: 'Messaggi' },
  ],
  COMMERCIALISTA: [
    { to: '/dashboard/commercialista', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/dashboard/commercialista/clienti', icon: Users, label: 'Clienti' },
    { to: '/dashboard/commercialista/pratiche', icon: FileText, label: 'Pratiche' },
    { to: '/dashboard/commercialista/documenti', icon: ClipboardCheck, label: 'Revisione Documenti' },
    { to: '/dashboard/commercialista/collaboratori', icon: UserCheck, label: 'Collaboratori' },
    { to: '/dashboard/commercialista/imposte', icon: Calculator, label: 'Calcolo Imposte' },
    { to: '/dashboard/commercialista/notifiche', icon: Bell, label: 'Notifiche' },
    { to: '/dashboard/commercialista/chat', icon: MessageSquare, label: 'Messaggi' },
  ],
  COLLABORATORE: [
    { to: '/dashboard/collaboratore', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/dashboard/collaboratore/pratiche', icon: FolderOpen, label: 'Pratiche Assegnate' },
    { to: '/dashboard/collaboratore/documenti', icon: ClipboardCheck, label: 'Revisione Documenti' },
    { to: '/dashboard/collaboratore/studi', icon: Building2, label: 'I miei Studi' },
    { to: '/dashboard/collaboratore/notifiche', icon: Bell, label: 'Notifiche' },
    { to: '/dashboard/collaboratore/chat', icon: MessageSquare, label: 'Messaggi' },
  ],
  AMMINISTRATORE: [
    { to: '/dashboard/admin', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/dashboard/admin/utenti', icon: Users, label: 'Gestione Utenti' },
    { to: '/dashboard/admin/notifiche', icon: Bell, label: 'Notifiche' },
  ],
};

/**
 * Barra di navigazione laterale dell'area autenticata.
 *
 * Presenta le voci di menu pertinenti al ruolo dell'utente, evidenzia la sezione
 * corrente e può essere compressa per ampliare l'area di contenuto.
 */
export default function Sidebar() {
  const { user, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const items = NAV_ITEMS[user?.ruolo] || [];

  const roleLabel = {
    CLIENTE: 'Cliente',
    COMMERCIALISTA: 'Commercialista',
    COLLABORATORE: 'Collaboratore',
    AMMINISTRATORE: 'Amministratore',
  };

  return (
    <aside
      className={`fixed left-0 top-0 h-screen bg-navy-900 text-white flex flex-col transition-all duration-300 z-40 ${
        collapsed ? 'w-[68px]' : 'w-[260px]'
      }`}
    >
      {/* Logo */}
      <div className="h-16 flex items-center px-5 border-b border-navy-700/50 shrink-0">
        {!collapsed ? (
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
              <span className="text-navy-900 font-extrabold text-sm tracking-tight">SF</span>
            </div>
            <div>
              <h1 className="text-[15px] font-bold tracking-tight text-white leading-none">
                StudioFiscale
              </h1>
              <span className="text-[10px] text-navy-300 tracking-widest uppercase font-medium">
                Gestionale
              </span>
            </div>
          </div>
        ) : (
          <div className="w-8 h-8 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm mx-auto">
            <span className="text-navy-900 font-extrabold text-sm">SF</span>
          </div>
        )}
      </div>

      {/* User info */}
      {!collapsed && user && (
        <div className="px-5 py-4 border-b border-navy-700/30">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-sm bg-navy-700 flex items-center justify-center text-sm font-semibold text-amber-400">
              {user.nome?.[0]}{user.cognome?.[0]}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-medium text-white truncate">
                {user.nome} {user.cognome}
              </p>
              <p className="text-[11px] text-navy-300 uppercase tracking-wider font-medium">
                {roleLabel[user.ruolo]}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 py-4 px-3 overflow-y-auto">
        <ul className="space-y-0.5">
          {items.map((item) => {
            const Icon = item.icon;
            const isActive =
              location.pathname === item.to ||
              (item.to !== `/dashboard/${user?.ruolo?.toLowerCase()}` &&
                item.to !== '/dashboard/admin' &&
                location.pathname.startsWith(item.to));

            return (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-sm text-sm font-medium transition-all duration-150 group ${
                    isActive
                      ? 'bg-navy-700/70 text-white'
                      : 'text-navy-200 hover:bg-navy-800 hover:text-white'
                  }`}
                  title={collapsed ? item.label : undefined}
                >
                  <Icon
                    size={18}
                    strokeWidth={1.75}
                    className={`shrink-0 transition-colors ${
                      isActive ? 'text-amber-400' : 'text-navy-400 group-hover:text-navy-200'
                    }`}
                  />
                  {!collapsed && <span>{item.label}</span>}
                  {isActive && (
                    <div className="absolute left-0 w-[3px] h-6 bg-amber-400 rounded-r-sm" />
                  )}
                </NavLink>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Bottom */}
      <div className="border-t border-navy-700/30 px-3 py-3 shrink-0">
        <button
          onClick={logout}
          className="flex items-center gap-3 px-3 py-2.5 w-full text-sm font-medium text-navy-300 hover:text-white hover:bg-navy-800 rounded-sm transition-all duration-150"
          title={collapsed ? 'Esci' : undefined}
        >
          <LogOut size={18} strokeWidth={1.75} className="shrink-0" />
          {!collapsed && <span>Esci</span>}
        </button>
      </div>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-20 w-6 h-6 bg-navy-700 border border-navy-600 rounded-full flex items-center justify-center text-navy-300 hover:text-white hover:bg-navy-600 transition-all z-50"
      >
        {collapsed ? <ChevronRight size={12} /> : <ChevronLeft size={12} />}
      </button>
    </aside>
  );
}
