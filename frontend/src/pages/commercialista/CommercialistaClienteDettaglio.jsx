import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import commercialistaService from '../../services/CommercialistaService.js';
import { ArrowLeft, Loader2, Calculator, Mail, FileText, FolderOpen, Euro } from 'lucide-react';

const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';
const formatEuro = (v) =>
  v != null ? new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(v) : '—';

/**
 * Scheda di dettaglio di un cliente vista dal commercialista: dati anagrafici e
 * fiscali, pratiche e documenti associati.
 */
export default function CommercialistaClienteDettaglio() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [cliente, setCliente] = useState(null);
  const [pratiche, setPratiche] = useState([]);
  const [documenti, setDocumenti] = useState([]);
  const [loading, setLoading] = useState(true);
  const [imposte, setImposte] = useState(null);
  const [calcolando, setCalcolando] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [c, p, d] = await Promise.all([
        commercialistaService.getCliente(id),
        commercialistaService.getClientePratiche(id),
        commercialistaService.getClienteDocumenti(id),
      ]);
      setCliente(c);
      setPratiche(p);
      setDocumenti(d);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento del cliente');
    } finally {
      setLoading(false);
    }
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const handleCalcola = async () => {
    setCalcolando(true);
    setImposte(null);
    try {
      setImposte(await commercialistaService.calcolaImposte(id));
    } catch (err) {
      toast.error(err.apiError?.message || err.message || 'Errore nel calcolo delle imposte');
    } finally {
      setCalcolando(false);
    }
  };

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento cliente...</span>
      </div>
    );
  }

  if (!cliente) {
    return (
      <div className="animate-fade-in">
        <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-anthracite-500 hover:text-navy-900 mb-4">
          <ArrowLeft size={15} /> Indietro
        </button>
        <p className="text-sm text-anthracite-400">Cliente non trovato.</p>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-anthracite-500 hover:text-navy-900 mb-4 transition-colors">
        <ArrowLeft size={15} /> Torna ai clienti
      </button>

      <PageTitle subtitle={<span className="inline-flex items-center gap-1.5"><Mail size={13} />{cliente.email}</span>}>
        {cliente.nomeCompleto}
      </PageTitle>

      {/* Profilo fiscale */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 mb-8">
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-anthracite-400 mb-1">Codice fiscale</p>
          <p className="text-sm font-medium text-navy-900">{cliente.codFiscale || '—'}</p>
        </div>
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-anthracite-400 mb-1">Partita IVA</p>
          <p className="text-sm font-medium text-navy-900">{cliente.pIVA || '—'}</p>
        </div>
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-anthracite-400 mb-1">Regime</p>
          <p className="text-sm font-medium text-navy-900">{cliente.regimeLabel}</p>
        </div>
        <div className="bg-white border border-anthracite-100 rounded-sm p-4">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-anthracite-400 mb-1">Reddito annuo</p>
          <p className="text-sm font-medium text-navy-900">{formatEuro(cliente.redditoAnnuo)}</p>
        </div>
      </div>

      {/* Calcolo imposte */}
      <div className="bg-navy-50 border border-navy-100 rounded-sm p-5 mb-8 flex items-center justify-between flex-wrap gap-4">
        <div className="flex items-center gap-3">
          <Euro size={20} className="text-navy-500" />
          <div>
            <p className="text-sm font-semibold text-navy-900">Calcolo imposte</p>
            <p className="text-xs text-anthracite-500">
              {imposte != null
                ? <>Imposte stimate: <strong className="text-navy-900">{formatEuro(imposte)}</strong></>
                : 'Stima basata su regime e reddito annuo'}
            </p>
          </div>
        </div>
        <Button
          variant="accent" icon={Calculator} loading={calcolando}
          disabled={!cliente.haDatiFiscali}
          onClick={handleCalcola}
        >
          Calcola imposte
        </Button>
      </div>
      {!cliente.haDatiFiscali && (
        <p className="text-xs text-amber-600 -mt-6 mb-8">
          Il cliente non ha regime o reddito annuo impostato: calcolo non disponibile.
        </p>
      )}

      {/* Pratiche */}
      <SectionTitle>
        <span className="flex items-center gap-2"><FolderOpen size={16} className="text-navy-500" />Pratiche ({pratiche.length})</span>
      </SectionTitle>
      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden mb-8">
        {pratiche.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-anthracite-400">Nessuna pratica</div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="bg-anthracite-50/70">
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Pratica</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Scadenza</th>
              </tr>
            </thead>
            <tbody>
              {pratiche.map(p => (
                <tr key={p.id} onClick={() => navigate(`/dashboard/commercialista/pratiche/${p.id}`)}
                    className="border-b border-anthracite-50 last:border-b-0 hover:bg-anthracite-50/40 cursor-pointer transition-colors">
                  <td className="px-5 py-3.5 text-sm font-medium text-navy-900">{p.tipoLabel}</td>
                  <td className="px-5 py-3.5"><StatoBadge stato={p.stato} /></td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatDate(p.scadenza)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Documenti */}
      <SectionTitle>
        <span className="flex items-center gap-2"><FileText size={16} className="text-navy-500" />Documenti ({documenti.length})</span>
      </SectionTitle>
      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
        {documenti.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-anthracite-400">Nessun documento</div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="bg-anthracite-50/70">
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Documento</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Tipo</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Caricato il</th>
              </tr>
            </thead>
            <tbody>
              {documenti.map(d => (
                <tr key={d.id} className="border-b border-anthracite-50 last:border-b-0">
                  <td className="px-5 py-3.5 text-sm font-medium text-navy-900">{d.nome}</td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-500">{d.tipoFile}</td>
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
