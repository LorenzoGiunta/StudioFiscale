import { useState, useEffect, useCallback } from 'react';
import { FileText, Clock, MessageSquare, AlertTriangle, Users, Loader2 } from 'lucide-react';
import { StatsCard, PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import praticaService from '../../services/PraticaService.js';
import notificaService from '../../services/NotificaService.js';
import { useNavigate } from 'react-router-dom';

/**
 * Dashboard del commercialista: quadro d'insieme di pratiche, scadenze imminenti,
 * clienti e attività dello studio, con scorciatoie alle sezioni operative.
 */
export default function CommercialistaDashboard() {
  const toast = useToast();
  const navigate = useNavigate();
  const [pratiche, setPratiche] = useState([]);
  const [notifiche, setNotifiche] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [praticheRes, notificheRes] = await Promise.allSettled([
        praticaService.getAll(0, 20),
        notificaService.getNonLette(0, 20),
      ]);
      // Entrambi restituiscono Page<T>: estraiamo .content
      if (praticheRes.status === 'fulfilled') setPratiche(praticheRes.value.content || []);
      if (notificheRes.status === 'fulfilled') setNotifiche(notificheRes.value.content || []);
    } catch {
      toast.error('Errore nel caricamento dei dati');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  // Finestra di scadenza (oggi → 7 giorni inclusi) delegata al model Pratica,
  // così da restare allineati con le notifiche del backend.
  const inScadenza = pratiche.filter(p => p.isInScadenza());

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  // Etichetta dei giorni mancanti, con casi speciali per oggi e domani
  const etichettaGiorni = (giorni) => {
    if (giorni === 0) return 'Oggi';
    if (giorni === 1) return 'Domani';
    return `${giorni} giorni`;
  };

  const clientiUnici = new Set(pratiche.map(p => p.nomeCliente).filter(Boolean)).size;

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
      <PageTitle subtitle="Panoramica dello studio">Dashboard</PageTitle>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatsCard icon={FileText} label="Pratiche totali" value={String(pratiche.length)} accent />
        <StatsCard icon={AlertTriangle} label="In scadenza (7gg)" value={String(inScadenza.length)} />
        <StatsCard icon={MessageSquare} label="Notifiche non lette" value={String(notifiche.length)} />
        <StatsCard icon={Users} label="Clienti attivi" value={String(clientiUnici)} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pratiche recenti */}
        <div className="lg:col-span-2">
          <SectionTitle action={<Button variant="ghost" size="sm" onClick={() => navigate('pratiche')}>Vedi tutte</Button>}>
            Pratiche recenti
          </SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
            {pratiche.length === 0 ? (
              <div className="px-5 py-8 text-center text-sm text-anthracite-400">Nessuna pratica trovata</div>
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

        {/* Scadenze imminenti */}
        <div>
          <SectionTitle>
            <span className="flex items-center gap-2">
              <Clock size={16} className="text-amber-500" />
              Scadenze imminenti
            </span>
          </SectionTitle>
          {inScadenza.length === 0 ? (
            <div className="bg-white border border-anthracite-100 rounded-sm px-5 py-6 text-center text-sm text-anthracite-400">
              Nessuna scadenza nei prossimi 7 giorni
            </div>
          ) : (
            <div className="space-y-2">
              {inScadenza.map(s => {
                const giorni = s.giorniAllaScadenza();
                const etichetta = etichettaGiorni(giorni);
                return (
                  <div key={s.id} className={`bg-white border rounded-sm p-4 transition-all hover:shadow-sm ${giorni <= 3 ? 'border-amber-200 bg-amber-50/20' : 'border-anthracite-100'}`}>
                    <p className="text-sm font-medium text-navy-900 mb-1">
                      {s.tipoPratica?.replace(/_/g, ' ')} — {s.nomeCliente || '—'}
                    </p>
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-anthracite-400">{formatDate(s.scadenza)}</span>
                      <span className={`text-[11px] font-semibold ${giorni <= 3 ? 'text-amber-600' : 'text-anthracite-400'}`}>
                        {giorni <= 3 ? `⚠ ${etichetta}` : etichetta}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
