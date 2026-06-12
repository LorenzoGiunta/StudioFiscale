import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import praticaService from '../../services/PraticaService.js';
import { ArrowLeft, FileText, Loader2, Check, CalendarClock, User, Users } from 'lucide-react';

// Ordine degli stati per la timeline
const FLUSSO_STATI = ['BOZZA', 'IN_LAVORAZIONE', 'IN_ATTESA_DOCUMENTI', 'COMPLETATA'];
const STATO_LABEL = {
  BOZZA: 'Bozza',
  IN_LAVORAZIONE: 'In lavorazione',
  IN_ATTESA_DOCUMENTI: 'In attesa documenti',
  COMPLETATA: 'Completata',
};

const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';

// Timeline orizzontale degli stati pratica
function Timeline({ statoCorrente }) {
  const idxCorrente = FLUSSO_STATI.indexOf(statoCorrente);
  return (
    <div className="flex items-center">
      {FLUSSO_STATI.map((stato, i) => {
        const completato = i < idxCorrente;
        const attivo = i === idxCorrente;
        return (
          <div key={stato} className="flex items-center flex-1 last:flex-none">
            <div className="flex flex-col items-center">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 ${
                completato ? 'bg-emerald-500 text-white'
                  : attivo ? 'bg-navy-900 text-white'
                  : 'bg-anthracite-100 text-anthracite-400'
              }`}>
                {completato ? <Check size={15} /> : i + 1}
              </div>
              <span className={`text-[10px] mt-1.5 text-center max-w-[80px] leading-tight ${
                attivo ? 'text-navy-900 font-semibold' : 'text-anthracite-400'
              }`}>
                {STATO_LABEL[stato]}
              </span>
            </div>
            {i < FLUSSO_STATI.length - 1 && (
              <div className={`flex-1 h-0.5 mx-2 mb-5 ${i < idxCorrente ? 'bg-emerald-500' : 'bg-anthracite-100'}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}

/**
 * Pagina di dettaglio di una pratica, condivisa tra i ruoli.
 *
 * Mostra dati, stato e documenti collegati alla pratica; le azioni disponibili
 * (avanzamento, assegnazione, gestione documenti) dipendono dal ruolo dell'utente.
 */
export default function PraticaDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [pratica, setPratica] = useState(null);
  const [documenti, setDocumenti] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [p, docs] = await Promise.all([
        praticaService.getById(id),
        praticaService.getDocumenti(id),
      ]);
      setPratica(p);
      setDocumenti(docs);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento della pratica');
    } finally {
      setLoading(false);
    }
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento pratica...</span>
      </div>
    );
  }

  if (!pratica) {
    return (
      <div className="animate-fade-in">
        <Button variant="ghost" icon={ArrowLeft} onClick={() => navigate(-1)}>Indietro</Button>
        <p className="text-sm text-anthracite-400 mt-6">Pratica non trovata.</p>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-anthracite-500 hover:text-navy-900 mb-4 transition-colors">
        <ArrowLeft size={15} /> Torna alle pratiche
      </button>

      <PageTitle subtitle={`Pratica #${pratica.id}`} actions={<StatoBadge stato={pratica.stato} />}>
        {pratica.tipoLabel}
      </PageTitle>

      {/* Info pratica */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <div className="flex items-center gap-2 mb-1 text-anthracite-400">
            <User size={14} /><span className="text-[11px] font-semibold uppercase tracking-wider">Cliente</span>
          </div>
          <p className="text-sm font-medium text-navy-900">{pratica.nomeCliente || '—'}</p>
        </div>
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <div className="flex items-center gap-2 mb-1 text-anthracite-400">
            <Users size={14} /><span className="text-[11px] font-semibold uppercase tracking-wider">Collaboratore</span>
          </div>
          <p className="text-sm font-medium text-navy-900">{pratica.nomeCollaboratore || 'Non assegnato'}</p>
        </div>
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <div className="flex items-center gap-2 mb-1 text-anthracite-400">
            <CalendarClock size={14} /><span className="text-[11px] font-semibold uppercase tracking-wider">Scadenza</span>
          </div>
          <p className="text-sm font-medium text-navy-900">{formatDate(pratica.scadenza)}</p>
        </div>
      </div>

      {/* Timeline stato */}
      <SectionTitle>Avanzamento</SectionTitle>
      <div className="bg-white border border-anthracite-100 rounded-sm p-6 mb-8">
        <Timeline statoCorrente={pratica.stato} />
      </div>

      {/* Documenti collegati */}
      <SectionTitle>Documenti collegati ({documenti.length})</SectionTitle>
      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
        {documenti.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-anthracite-400">
            Nessun documento collegato a questa pratica
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="bg-anthracite-50/70">
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Documento</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Tipo</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Versione</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Caricato il</th>
              </tr>
            </thead>
            <tbody>
              {documenti.map(d => (
                <tr key={d.id} className="border-b border-anthracite-50 last:border-b-0">
                  <td className="px-5 py-3.5 text-sm font-medium text-navy-900 flex items-center gap-2">
                    <FileText size={15} className="text-anthracite-400" />{d.nome}
                  </td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-500">{d.tipoFile}</td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-500">v{d.versione}</td>
                  <td className="px-5 py-3.5">
                    <span className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold rounded-sm ${
                      d.approvato ? 'text-emerald-600 bg-emerald-50'
                        : d.rifiutato ? 'text-red-600 bg-red-50'
                        : 'text-amber-600 bg-amber-50'
                    }`}>
                      {d.statoLabel}
                    </span>
                  </td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatDate(d.dataCaricamento)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
