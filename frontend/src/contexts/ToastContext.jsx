import { createContext, useContext, useState, useCallback } from 'react';

/**
 * Contesto per i messaggi temporanei (toast).
 *
 * Mette a disposizione dell'intera applicazione le funzioni per mostrare
 * notifiche transitorie di esito (successo, errore, avviso, informazione), che
 * compaiono e si rimuovono automaticamente dopo una durata prestabilita.
 */
const ToastContext = createContext(null);

let toastId = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = ++toastId;
    setToasts((prev) => [...prev, { id, message, type, exiting: false }]);
    setTimeout(() => {
      setToasts((prev) =>
        prev.map((t) => (t.id === id ? { ...t, exiting: true } : t))
      );
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
      }, 250);
    }, duration);
  }, []);

  const success = useCallback((msg) => addToast(msg, 'success'), [addToast]);
  const error = useCallback((msg) => addToast(msg, 'error'), [addToast]);
  const info = useCallback((msg) => addToast(msg, 'info'), [addToast]);
  const warning = useCallback((msg) => addToast(msg, 'warning'), [addToast]);

  return (
    <ToastContext.Provider value={{ addToast, success, error, info, warning }}>
      {children}
      <ToastContainer toasts={toasts} />
    </ToastContext.Provider>
  );
}

function ToastContainer({ toasts }) {
  if (toasts.length === 0) return null;

  const typeStyles = {
    success: 'border-l-[3px] border-l-emerald-500 bg-white',
    error: 'border-l-[3px] border-l-red-500 bg-white',
    warning: 'border-l-[3px] border-l-amber-500 bg-white',
    info: 'border-l-[3px] border-l-navy-400 bg-white',
  };

  const iconMap = {
    success: (
      <svg className="w-4 h-4 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
      </svg>
    ),
    error: (
      <svg className="w-4 h-4 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
      </svg>
    ),
    warning: (
      <svg className="w-4 h-4 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
      </svg>
    ),
    info: (
      <svg className="w-4 h-4 text-navy-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  };

  return (
    <div className="fixed top-4 right-4 z-[9999] flex flex-col gap-2 min-w-[320px] max-w-[420px]">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`${typeStyles[t.type]} ${
            t.exiting ? 'animate-toast-out' : 'animate-toast-in'
          } shadow-sm px-4 py-3 flex items-start gap-3 rounded-sm`}
        >
          <span className="mt-0.5 shrink-0">{iconMap[t.type]}</span>
          <p className="text-sm text-navy-800 leading-snug">{t.message}</p>
        </div>
      ))}
    </div>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
