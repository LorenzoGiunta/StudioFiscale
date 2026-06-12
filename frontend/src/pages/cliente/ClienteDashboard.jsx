import { useState, useEffect, useCallback } from 'react';
import { FileText, Upload, Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { StatsCard, PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import clienteService from '../../services/ClienteService.js';
import notificaService from '../../services/NotificaService.js';

/**
 * Dashboard del cliente: riepilogo sintetico di pratiche, documenti e notifiche,
 * con accesso rapido alle sezioni principali della propria area.
 */
export default function ClienteDashboard() {
  const toast = useToast();
  const navigate = useNavigate();
  const [pratiche, setPratiche] = useState([]);
  const [notifiche, setNotifiche] = useState([]);
  const [docCount, setDocCount] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [praticheData, docData, notificheData] = await Promise.allSettled([
        clienteService.getPratiche(),
        clienteService.getDocumenti(),
        notificaService.getNonLette(0, 20),
      ]);
      if (praticheData.status === 'fulfilled') setPratiche(praticheData.value);
      if (docData.status === 'fulfilled') setDocCount(docData.value.length);
      // fetchNotificheNonLette ora restituisce Page<T>: estraiamo .content
      if (notificheData.status === 'fulfilled') setNotifiche(notificheData.value.content || []);
    } catch (err) {
      toast.error('Errore nel caricamento dei dati');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { loadData(); }, [loadData]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  return (
    <div className="animate-fade-in">
      <PageTitle subtitle="Panoramica delle tue pratiche fiscali">
        Dashboard
      </PageTitle>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        <StatsCard icon={FileText} label="Pratiche attive" value={String(pratiche.length)} accent />
        <StatsCard icon={Upload} label="Documenti caricati" value={String(docCount)} />
        <StatsCard icon={Bell} label="Notifiche non lette" value={String(notifiche.length)} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pratiche */}
        <div className="lg:col-span-2">
          <SectionTitle action={<Button variant="ghost" size="sm" onClick={() => navigate('pratiche')}>Vedi tutte</Button>}>
            Pratiche recenti
          </SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
            {loading ? (
              <div className="px-5 py-8 text-center text-sm text-anthracite-400">Caricamento...</div>
            ) : pratiche.length === 0 ? (
              <div className="px-5 py-8 text-center text-sm text-anthracite-400">Nessuna pratica trovata</div>
            ) : (
              <table className="w-full">
                <thead>
                  <tr className="bg-anthracite-50/70">
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Pratica</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden sm:table-cell">Collaboratore</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Creata</th>
                  </tr>
                </thead>
                <tbody>
                  {pratiche.slice(0, 5).map(p => (
                    <tr key={p.id} onClick={() => navigate(`pratiche/${p.id}`)} className="border-b border-anthracite-50 last:border-b-0 hover:bg-anthracite-50/40 transition-colors cursor-pointer">
                      <td className="px-5 py-3.5 text-sm font-medium text-navy-900">{p.tipoPratica?.replace(/_/g, ' ') || '—'}</td>
                      <td className="px-5 py-3.5"><StatoBadge stato={p.stato} /></td>
                      <td className="px-5 py-3.5 text-sm text-anthracite-500 hidden sm:table-cell">{p.nomeCollaboratore || '—'}</td>
                      <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatDate(p.dataCreazione)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Sidebar notifiche */}
        <div>
          <SectionTitle action={<Button variant="ghost" size="sm" onClick={() => navigate('notifiche')}>Vedi tutte</Button>}>
            Notifiche recenti
          </SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm">
            {notifiche.length === 0 ? (
              <div className="px-5 py-4 text-sm text-anthracite-400">Nessuna notifica non letta</div>
            ) : (
              notifiche.slice(0, 5).map(n => (
                <div key={n.id} className="px-5 py-3.5 border-b border-anthracite-50 last:border-b-0">
                  <p className="text-sm text-navy-800 leading-snug">{n.messaggio}</p>
                  <p className="text-[11px] text-anthracite-400 mt-1">{formatDate(n.dataCreazione)}</p>
                </div>
              ))
            )}
          </div>

          {/* Quick upload */}
          <div className="mt-6">
            <SectionTitle>Caricamento rapido</SectionTitle>
            <button
              onClick={() => navigate('documenti')}
              className="w-full bg-white border border-dashed border-anthracite-200 rounded-sm p-6 text-center hover:border-navy-400 hover:bg-navy-50/30 transition-all group"
            >
              <Upload size={24} strokeWidth={1.5} className="text-anthracite-300 group-hover:text-navy-500 mx-auto mb-2 transition-colors" />
              <p className="text-sm font-medium text-anthracite-500 group-hover:text-navy-700 transition-colors">
                Vai a Documenti per caricare
              </p>
              <p className="text-[11px] text-anthracite-300 mt-1">PDF, DOC, XLS</p>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
