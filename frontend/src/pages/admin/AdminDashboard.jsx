import { useState, useEffect, useCallback } from 'react';
import { Users, Shield, UserCheck, UserX, Loader2, FileText, FolderOpen, Trash2, Clock } from 'lucide-react';
import { StatsCard, PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import adminService from '../../services/AdminService.js';
import { useNavigate } from 'react-router-dom';

const STATO_PRATICA_LABEL = {
  BOZZA: 'Bozza',
  IN_LAVORAZIONE: 'In lavorazione',
  IN_ATTESA_DOCUMENTI: 'In attesa documenti',
  COMPLETATA: 'Completata',
};

const STATO_DOC_LABEL = {
  IN_REVISIONE: 'In revisione',
  APPROVATO: 'Approvato',
  RIFIUTATO: 'Rifiutato',
};

const RUOLO_LABEL = {
  CLIENTE: 'Clienti',
  COMMERCIALISTA: 'Commercialisti',
  COLLABORATORE: 'Collaboratori',
  AMMINISTRATORE: 'Amministratori',
};

// Barra di ripartizione: una riga per chiave con conteggio e barra proporzionale
function Ripartizione({ dati = {}, labels, totale, colore = 'bg-navy-500' }) {
  const entries = Object.entries(dati);
  if (entries.length === 0) {
    return <p className="text-sm text-anthracite-400 px-5 py-4">Nessun dato</p>;
  }
  return (
    <div className="space-y-3 p-5">
      {entries.map(([chiave, valore]) => {
        const perc = totale > 0 ? Math.round((valore / totale) * 100) : 0;
        return (
          <div key={chiave}>
            <div className="flex items-center justify-between mb-1">
              <span className="text-sm text-navy-800">{labels[chiave] || chiave}</span>
              <span className="text-sm font-semibold text-navy-900">{valore}</span>
            </div>
            <div className="h-1.5 bg-anthracite-100 rounded-full overflow-hidden">
              <div className={`h-full ${colore} rounded-full`} style={{ width: `${perc}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

/**
 * Dashboard dell'amministratore: presenta le statistiche aggregate del sistema
 * (utenti, pratiche e documenti) come quadro d'insieme.
 */
export default function AdminDashboard() {
  const toast = useToast();
  const navigate = useNavigate();
  const [stat, setStat] = useState(null);
  const [ultimaAzione, setUltimaAzione] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      // Le statistiche reggono la pagina; l'ultima azione è un'informazione
      // accessoria, perciò un suo eventuale errore non blocca il caricamento.
      const [statRes, azioneRes] = await Promise.allSettled([
        adminService.getStatistiche(),
        adminService.getUltimaAzione(),
      ]);
      if (statRes.status === 'fulfilled') setStat(statRes.value);
      else toast.error('Errore nel caricamento delle statistiche');
      if (azioneRes.status === 'fulfilled') setUltimaAzione(azioneRes.value.ultimaAzione ?? null);
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  // Data e ora dell'ultima operazione amministrativa, o un segnaposto se assente
  const formatDateTime = (d) =>
    d ? new Date(d).toLocaleString('it-IT', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    }) : 'Nessuna azione registrata';

  if (loading || !stat) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento dati...</span>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle="Panoramica e metriche del sistema"
        actions={<Button variant="ghost" size="sm" onClick={() => navigate('utenti')}>Gestione utenti</Button>}
      >
        Dashboard Amministratore
      </PageTitle>

      {/* Ultima azione amministrativa dell'amministratore corrente */}
      <p className="text-xs text-anthracite-400 -mt-4 mb-6 flex items-center gap-1.5">
        <Clock size={13} />
        Ultima azione amministrativa:
        <span className="font-medium text-navy-700">{formatDateTime(ultimaAzione)}</span>
      </p>

      {/* Metriche utenti */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatsCard icon={Users} label="Utenti totali" value={String(stat.utentiTotali)} accent />
        <StatsCard icon={UserCheck} label="Abilitati" value={String(stat.utentiAbilitati)} />
        <StatsCard icon={UserX} label="Disabilitati" value={String(stat.utentiDisabilitati)} />
        <StatsCard icon={Trash2} label="Eliminati" value={String(stat.utentiEliminati)} />
      </div>

      {/* Metriche pratiche e documenti */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatsCard icon={FolderOpen} label="Pratiche totali" value={String(stat.praticheTotali)} />
        <StatsCard icon={FileText} label="Documenti totali" value={String(stat.documentiTotali)} />
        <StatsCard icon={Shield} label="Amministratori" value={String(stat.utentiPerRuolo?.AMMINISTRATORE || 0)} />
        <StatsCard icon={Users} label="Clienti" value={String(stat.utentiPerRuolo?.CLIENTE || 0)} />
      </div>

      {/* Ripartizioni */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div>
          <SectionTitle>Utenti per ruolo</SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm">
            <Ripartizione dati={stat.utentiPerRuolo} labels={RUOLO_LABEL} totale={stat.utentiTotali} colore="bg-navy-500" />
          </div>
        </div>
        <div>
          <SectionTitle>Pratiche per stato</SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm">
            <Ripartizione dati={stat.pratichePerStato} labels={STATO_PRATICA_LABEL} totale={stat.praticheTotali} colore="bg-amber-500" />
          </div>
        </div>
        <div>
          <SectionTitle>Documenti per stato</SectionTitle>
          <div className="bg-white border border-anthracite-100 rounded-sm">
            <Ripartizione dati={stat.documentiPerStato} labels={STATO_DOC_LABEL} totale={stat.documentiTotali} colore="bg-emerald-500" />
          </div>
        </div>
      </div>
    </div>
  );
}
