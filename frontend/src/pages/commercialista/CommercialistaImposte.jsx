import { useState, useEffect, useCallback } from 'react';
import { PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { Button, Select } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import commercialistaService from '../../services/CommercialistaService.js';
import { Calculator, RefreshCw, Loader2, TrendingUp, Euro } from 'lucide-react';

/**
 * Pagina di calcolo delle imposte: il commercialista seleziona un cliente e
 * ottiene dal backend l'importo dovuto in base al regime fiscale.
 */
export default function CommercialistaImposte() {
  const toast = useToast();
  const [clienti, setClienti] = useState([]);
  const [clienteId, setClienteId] = useState('');
  const [risultato, setRisultato] = useState(null);
  const [loadingClienti, setLoadingClienti] = useState(true);
  const [calcolando, setCalcolando] = useState(false);

  const loadClienti = useCallback(async () => {
    setLoadingClienti(true);
    try {
      const data = await commercialistaService.getClienti();
      setClienti((data || []).filter(c => c.enabled));
    } catch {
      toast.error('Errore nel caricamento dei clienti');
    } finally {
      setLoadingClienti(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadClienti(); }, [loadClienti]);

  const handleCalcola = async () => {
    if (!clienteId) {
      toast.warning('Seleziona un cliente');
      return;
    }
    setCalcolando(true);
    setRisultato(null);
    try {
      const importo = await commercialistaService.calcolaImposte(clienteId);
      setRisultato(importo);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore nel calcolo delle imposte';
      toast.error(msg);
    } finally {
      setCalcolando(false);
    }
  };

  const clienteSelezionato = clienti.find(c => String(c.id) === String(clienteId));

  const formatEuro = (val) =>
    new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(val);

  return (
    <div className="animate-fade-in">
      <PageTitle subtitle="Simulazione calcolo imposte per regime ordinario e forfettario">
        Calcolo Imposte
      </PageTitle>

      {/* Selezione cliente */}
      <div className="bg-white border border-anthracite-100 rounded-sm p-6 mb-6">
        <SectionTitle>Seleziona cliente</SectionTitle>
        <div className="flex items-end gap-4 flex-wrap">
          {loadingClienti ? (
            <div className="flex items-center gap-2 text-sm text-anthracite-400">
              <Loader2 size={16} className="animate-spin" />
              Caricamento clienti...
            </div>
          ) : (
            <div className="flex-1 min-w-[280px]">
              <label className="block text-xs font-medium text-anthracite-500 mb-1.5">Cliente *</label>
              <Select
                id="select-cliente"
                value={clienteId}
                onChange={(e) => { setClienteId(e.target.value); setRisultato(null); }}
                className="w-full"
              >
                <option value="">Seleziona un cliente...</option>
                {clienti.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.nome} {c.cognome} — {c.email}
                  </option>
                ))}
              </Select>
            </div>
          )}
          <Button
            variant="accent"
            icon={Calculator}
            onClick={handleCalcola}
            loading={calcolando}
            disabled={!clienteId || loadingClienti}
          >
            Calcola imposte
          </Button>
          <Button variant="ghost" icon={RefreshCw} onClick={() => { setRisultato(null); setClienteId(''); }} />
        </div>
      </div>

      {/* Risultato */}
      {calcolando && (
        <div className="flex items-center justify-center py-16">
          <Loader2 size={24} className="animate-spin text-navy-400 mr-3" />
          <span className="text-sm text-anthracite-400">Calcolo in corso...</span>
        </div>
      )}

      {risultato !== null && risultato !== undefined && !calcolando && (
        <div className="animate-fade-in space-y-4">
          {/* Header risultato */}
          <div className="bg-navy-900 text-white rounded-sm p-6 flex items-center gap-5">
            <div className="w-14 h-14 bg-amber-400 rounded-sm flex items-center justify-center shrink-0">
              <Euro size={24} className="text-navy-900" />
            </div>
            <div>
              <p className="text-sm text-navy-300 mb-1">
                Imposte stimate per{' '}
                <strong className="text-white">
                  {clienteSelezionato?.nome} {clienteSelezionato?.cognome}
                </strong>
              </p>
              <p className="text-3xl font-bold text-amber-400">{formatEuro(risultato)}</p>
            </div>
          </div>

          {/* Dettaglio info */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="bg-white border border-anthracite-100 rounded-sm p-5">
              <div className="flex items-center gap-2 mb-2">
                <TrendingUp size={16} className="text-navy-400" />
                <span className="text-xs font-semibold text-anthracite-500 uppercase tracking-wider">Importo calcolato</span>
              </div>
              <p className="text-2xl font-bold text-navy-900">{formatEuro(risultato)}</p>
              <p className="text-xs text-anthracite-400 mt-1">Stima annuale</p>
            </div>
            <div className="bg-white border border-anthracite-100 rounded-sm p-5">
              <div className="flex items-center gap-2 mb-2">
                <Euro size={16} className="text-navy-400" />
                <span className="text-xs font-semibold text-anthracite-500 uppercase tracking-wider">Cliente</span>
              </div>
              <p className="text-sm font-semibold text-navy-900">
                {clienteSelezionato?.nome} {clienteSelezionato?.cognome}
              </p>
              <p className="text-xs text-anthracite-400 mt-1">{clienteSelezionato?.email}</p>
            </div>
          </div>


          {/* Nota disclaimer */}
          <div className="bg-amber-50 border border-amber-200 rounded-sm p-4 flex items-start gap-3">
            <Calculator size={16} className="text-amber-600 mt-0.5 shrink-0" />
            <p className="text-xs text-amber-700 leading-relaxed">
              Il calcolo è una <strong>simulazione</strong> basata sui dati fiscali del cliente (regime, reddito annuo, P.IVA).
              L'importo effettivo può variare in base a deduzioni, crediti d'imposta e aggiornamenti normativi.
            </p>
          </div>
        </div>
      )}

      {/* Stato vuoto */}
      {risultato === null && !calcolando && (
        <div className="flex flex-col items-center justify-center py-20 bg-white border border-dashed border-anthracite-200 rounded-sm">
          <div className="w-16 h-16 bg-anthracite-50 rounded-sm flex items-center justify-center mb-4">
            <Calculator size={28} className="text-anthracite-300" />
          </div>
          <p className="text-sm font-medium text-navy-900 mb-1">Nessun calcolo eseguito</p>
          <p className="text-xs text-anthracite-400 text-center max-w-xs">
            Seleziona un cliente e premi "Calcola imposte" per ottenere la stima.
          </p>
        </div>
      )}
    </div>
  );
}
