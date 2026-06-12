import { useState, useEffect, useCallback } from 'react';
import { Bell, Check, FileText, CheckCircle, XCircle, AlertCircle, UserPlus, ShieldAlert, UserCheck, Loader2, RefreshCw } from 'lucide-react';
import { PageTitle } from './ui/Display.jsx';
import { Button } from './ui/FormControls.jsx';
import { useToast } from '../contexts/ToastContext.jsx';
import notificaService from '../services/NotificaService.js';

// Le chiavi corrispondono ai valori dell'enum TipoNotifica del backend
const tipoIcons = {
  CAMBIO_STATO: FileText,
  DOCUMENTO_CARICATO: FileText,
  DOCUMENTO_APPROVATO: CheckCircle,
  DOCUMENTO_RIFIUTATO: XCircle,
  SCADENZA_IMMINENTE: AlertCircle,
  INVITO_COLLABORAZIONE: UserPlus,
  ACCOUNT_DISABILITATO: ShieldAlert,
  ACCOUNT_CREATO: UserCheck,
};

const tipoColors = {
  CAMBIO_STATO: 'text-blue-500 bg-blue-50',
  DOCUMENTO_CARICATO: 'text-navy-500 bg-navy-50',
  DOCUMENTO_APPROVATO: 'text-emerald-500 bg-emerald-50',
  DOCUMENTO_RIFIUTATO: 'text-red-500 bg-red-50',
  SCADENZA_IMMINENTE: 'text-amber-500 bg-amber-50',
  INVITO_COLLABORAZIONE: 'text-blue-500 bg-blue-50',
  ACCOUNT_DISABILITATO: 'text-red-500 bg-red-50',
  ACCOUNT_CREATO: 'text-emerald-500 bg-emerald-50',
};

// Titolo mostrato per ciascun tipo: la risposta del backend non include un
// campo dedicato, quindi lo si deriva dal tipo (coerente con gli oggetti email)
const tipoTitoli = {
  CAMBIO_STATO: 'Aggiornamento pratica',
  DOCUMENTO_CARICATO: 'Nuovo documento',
  DOCUMENTO_APPROVATO: 'Documento approvato',
  DOCUMENTO_RIFIUTATO: 'Documento rifiutato',
  SCADENZA_IMMINENTE: 'Scadenza imminente',
  INVITO_COLLABORAZIONE: 'Invito di collaborazione',
  ACCOUNT_DISABILITATO: 'Account disabilitato',
  ACCOUNT_CREATO: 'Account creato',
};

/**
 * Vista delle notifiche dell'utente.
 *
 * Elenca le notifiche ricevute con la relativa icona per tipo, distingue quelle
 * non lette e consente di marcarle come lette singolarmente o tutte insieme.
 */
export default function NotificheView() {
  const toast = useToast();
  const [notifiche, setNotifiche] = useState([]);
  const [loading, setLoading] = useState(true);
  const [markingAll, setMarkingAll] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      // fetchNotifiche ora restituisce un Page<T>: estraiamo .content
      const pageData = await notificaService.getAll(0, 50);
      setNotifiche(pageData.content || []);
    } catch {
      toast.error('Errore nel caricamento delle notifiche');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const segnaLetta = async (id) => {
    try {
      await notificaService.markLetta(id);
      setNotifiche(prev => prev.map(n => n.id === id ? { ...n, letta: true } : n));
    } catch {
      toast.error('Errore nel segnare la notifica come letta');
    }
  };

  const segnaTutteLette = async () => {
    setMarkingAll(true);
    try {
      await notificaService.markTutteLette();
      setNotifiche(prev => prev.map(n => ({ ...n, letta: true })));
      toast.success('Tutte le notifiche segnate come lette');
    } catch {
      toast.error('Errore nel segnare le notifiche come lette');
    } finally {
      setMarkingAll(false);
    }
  };

  const formatData = (d) => {
    const date = new Date(d);
    const now = new Date();
    const diff = now - date;
    if (diff < 86400000) return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
    if (diff < 172800000) return 'Ieri';
    return date.toLocaleDateString('it-IT', { day: '2-digit', month: 'short' });
  };

  const nonLette = notifiche.filter(n => !n.letta).length;

  return (
    <div>
      <PageTitle
        subtitle={loading ? 'Caricamento...' : `${nonLette} notifiche non lette`}
        actions={
          <div className="flex items-center gap-2">
            <Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />
            {nonLette > 0 && (
              <Button variant="ghost" size="sm" icon={Check} onClick={segnaTutteLette} loading={markingAll}>
                Segna tutte come lette
              </Button>
            )}
          </div>
        }
      >
        Notifiche
      </PageTitle>

      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
        {loading ? (
          <div className="px-5 py-12 flex items-center justify-center">
            <Loader2 size={20} className="animate-spin text-navy-400 mr-2" />
            <span className="text-sm text-anthracite-400">Caricamento notifiche...</span>
          </div>
        ) : notifiche.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <Bell size={32} className="text-anthracite-200 mx-auto mb-3" />
            <p className="text-sm text-anthracite-400">Nessuna notifica</p>
          </div>
        ) : (
          notifiche.map(n => {
            const tipo = n.tipo?.toUpperCase() || '';
            const Icon = tipoIcons[tipo] || Bell;
            const colorClass = tipoColors[tipo] || 'text-anthracite-400 bg-anthracite-50';
            const titolo = tipoTitoli[tipo] || 'Notifica';
            return (
              <div
                key={n.id}
                className={`flex items-start gap-4 px-5 py-4 border-b border-anthracite-50 last:border-b-0 transition-colors ${
                  n.letta ? 'bg-white' : 'bg-amber-50/30'
                }`}
              >
                <div className={`w-9 h-9 rounded-sm flex items-center justify-center shrink-0 ${colorClass}`}>
                  <Icon size={16} strokeWidth={1.75} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className={`text-sm font-medium ${n.letta ? 'text-navy-800' : 'text-navy-900'}`}>
                      {titolo}
                    </p>
                    {!n.letta && <span className="w-2 h-2 bg-amber-500 rounded-full shrink-0" />}
                  </div>
                  <p className="text-xs text-anthracite-400 mt-0.5">{n.messaggio}</p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="text-[11px] text-anthracite-400">{formatData(n.dataCreazione)}</span>
                  {!n.letta && (
                    <button
                      onClick={() => segnaLetta(n.id)}
                      className="p-1 text-anthracite-300 hover:text-navy-900 hover:bg-anthracite-50 rounded-sm transition-colors"
                      title="Segna come letta"
                    >
                      <Check size={14} />
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
