import { useState, useEffect, useCallback } from 'react';
import { FileText, ClipboardCheck, Bell, Loader2 } from 'lucide-react';
import { StatsCard, PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import collaboratoreService from '../../services/CollaboratoreService.js';
import notificaService from '../../services/NotificaService.js';
import { useNavigate } from 'react-router-dom';

/**
 * Dashboard del collaboratore: riepilogo delle pratiche assegnate, dei documenti
 * da revisionare e delle notifiche, con accesso rapido alle relative sezioni.
 */
export default function CollaboratoreDashboard() {
  const toast = useToast();
  const navigate = useNavigate();
  const [pratiche, setPratiche] = useState([]);
  const [documenti, setDocumenti] = useState([]);
  const [notifiche, setNotifiche] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [pRes, dRes, nRes] = await Promise.allSettled([
        collaboratoreService.getPratiche(),
        collaboratoreService.getDocumenti(),
        notificaService.getNonLette(0, 20),
      ]);
      if (pRes.status === 'fulfilled') setPratiche(pRes.value);
      if (dRes.status === 'fulfilled') setDocumenti(dRes.value);
      // fetchNotificheNonLette ora restituisce Page<T>: estraiamo .content
      if (nRes.status === 'fulfilled') setNotifiche(nRes.value.content || []);
    } catch {
      toast.error('Errore nel caricamento dei dati');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento dati...</span>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <PageTitle subtitle="Le tue attività assegnate">Dashboard</PageTitle>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        <StatsCard icon={FileText} label="Pratiche assegnate" value={String(pratiche.length)} accent />
        <StatsCard icon={ClipboardCheck} label="Documenti da revisionare" value={String(documenti.length)} />
        <StatsCard icon={Bell} label="Notifiche non lette" value={String(notifiche.length)} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <SectionTitle action={<Button variant="ghost" size="sm" onClick={() => navigate('pratiche')}>Vedi tutte</Button>}>
            Pratiche assegnate
          </SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
            {pratiche.length === 0 ? (
              <div className="px-5 py-8 text-center text-sm text-anthracite-400">Nessuna pratica assegnata</div>
            ) : (
              <table className="w-full">
                <thead>
                  <tr className="bg-anthracite-50/70">
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Cliente</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Pratica</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Scadenza</th>
                  </tr>
                </thead>
                <tbody>
                  {pratiche.slice(0, 5).map(p => (
                    <tr key={p.id} onClick={() => navigate(`pratiche/${p.id}`)} className="border-b border-anthracite-50 last:border-b-0 hover:bg-anthracite-50/40 transition-colors cursor-pointer">
                      <td className="px-5 py-3.5 text-sm font-medium text-navy-900">{p.nomeCliente || '—'}</td>
                      <td className="px-5 py-3.5 text-sm text-anthracite-600">{p.tipoPratica?.replace(/_/g, ' ') || '—'}</td>
                      <td className="px-5 py-3.5"><StatoBadge stato={p.stato} /></td>
                      <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatDate(p.scadenza)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div>
          <SectionTitle action={<Button variant="ghost" size="sm" onClick={() => navigate('documenti')}>Vedi tutti</Button>}>
            Documenti da revisionare
          </SectionTitle>
          {documenti.length === 0 ? (
            <div className="bg-white border border-anthracite-100 rounded-sm px-5 py-6 text-center text-sm text-anthracite-400">
              Nessun documento da revisionare
            </div>
          ) : (
            <div className="space-y-2">
              {documenti.slice(0, 5).map(d => (
                <div key={d.id} onClick={() => navigate('documenti')} className="bg-white border border-anthracite-100 rounded-sm p-4 hover:shadow-sm transition-all cursor-pointer">
                  <div className="flex items-center gap-3">
                    <FileText size={16} className="text-navy-400 shrink-0" />
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-navy-900 truncate">{d.nome}</p>
                      <p className="text-xs text-anthracite-400">{d.tipoFile} · {formatDate(d.dataCaricamento)}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
