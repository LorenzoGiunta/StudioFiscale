import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { RuoloBadge } from '../../components/ui/Badge.jsx';
import { Button, Textarea } from '../../components/ui/FormControls.jsx';
import DataTable from '../../components/ui/DataTable.jsx';
import Modal from '../../components/ui/Modal.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import adminService from '../../services/AdminService.js';
import { UserCheck, UserX, Loader2, RefreshCw, ClipboardCheck, Copy, Check } from 'lucide-react';

/**
 * Pagina delle richieste di iscrizione in attesa di verifica.
 *
 * I commercialisti che si registrano non possono accedere finché un
 * amministratore non ne controlla a mano il numero di albo (non esistono API
 * pubbliche per farlo in automatico). Qui l'amministratore vede i dati della
 * richiesta, con il numero di albo in evidenza, e la approva o la rifiuta,
 * indicando facoltativamente una motivazione che verrà recapitata all'utente.
 */
export default function AdminRichiesteIscrizione() {
  const toast = useToast();
  const [richieste, setRichieste] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState(null);
  const [copiato, setCopiato] = useState(null);

  // Modale di rifiuto con motivazione facoltativa
  const [daRifiutare, setDaRifiutare] = useState(null);
  const [motivazione, setMotivazione] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setRichieste(await adminService.getRichiesteIscrizione() || []);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento delle richieste');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const handleApprova = async (r) => {
    if (!window.confirm(`Confermi di aver verificato il numero di albo ${r.numeroAlbo || ''} di ${r.nome} ${r.cognome}?\nL'account verrà attivato e l'utente avvisato via email.`)) return;
    setActionId(r.id);
    try {
      await adminService.approvaRichiesta(r.id);
      toast.success(`Iscrizione di ${r.nome} ${r.cognome} approvata`);
      await loadData();
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nell\'approvazione della richiesta');
    } finally {
      setActionId(null);
    }
  };

  const apriRifiuto = (r) => {
    setDaRifiutare(r);
    setMotivazione('');
  };

  const handleRifiuta = async () => {
    if (!daRifiutare) return;
    setActionId(daRifiutare.id);
    try {
      await adminService.rifiutaRichiesta(daRifiutare.id, motivazione.trim() || null);
      toast.success(`Iscrizione di ${daRifiutare.nome} ${daRifiutare.cognome} rifiutata`);
      setDaRifiutare(null);
      await loadData();
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel rifiuto della richiesta');
    } finally {
      setActionId(null);
    }
  };

  // Copia il numero di albo per incollarlo nel motore di ricerca dell'Ordine
  const copiaAlbo = async (r) => {
    try {
      await navigator.clipboard.writeText(r.numeroAlbo || '');
      setCopiato(r.id);
      setTimeout(() => setCopiato(null), 1500);
    } catch {
      toast.error('Impossibile copiare negli appunti');
    }
  };

  const formatDateTime = (d) =>
    d ? new Date(d).toLocaleString('it-IT', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    }) : '—';

  const columns = [
    { key: 'nome', label: 'Richiedente', render: (_, row) => (
      <div>
        <p className="text-sm font-medium text-navy-900">{row.nome} {row.cognome}</p>
        <p className="text-xs text-anthracite-400">{row.email}</p>
      </div>
    )},
    { key: 'ruolo', label: 'Ruolo', render: (_, row) => <RuoloBadge ruolo={row.ruolo} />, width: '140px' },
    { key: 'numeroAlbo', label: 'Numero albo', render: (_, row) => (
      row.numeroAlbo ? (
        <div className="flex items-center gap-2">
          <span className="font-mono text-sm font-semibold text-navy-900 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-sm">
            {row.numeroAlbo}
          </span>
          <button
            onClick={() => copiaAlbo(row)}
            className="p-1 text-anthracite-300 hover:text-navy-900 hover:bg-anthracite-50 rounded-sm transition-colors"
            title="Copia numero di albo"
          >
            {copiato === row.id ? <Check size={13} className="text-emerald-600" /> : <Copy size={13} />}
          </button>
        </div>
      ) : <span className="text-xs text-anthracite-300">—</span>
    ), width: '200px' },
    { key: 'dataRegistrazione', label: 'Richiesta il', render: (v) => (
      <span className="text-sm text-anthracite-500">{formatDateTime(v)}</span>
    ), width: '160px' },
    { key: 'azioni', label: 'Verifica', render: (_, row) => (
      <div className="flex items-center gap-1">
        <Button variant="ghost" size="sm" icon={UserCheck}
          loading={actionId === row.id}
          onClick={() => handleApprova(row)}>
          Approva
        </Button>
        <Button variant="ghost" size="sm" icon={UserX}
          disabled={actionId === row.id}
          onClick={() => apriRifiuto(row)}>
          Rifiuta
        </Button>
      </div>
    ), width: '200px' },
  ];

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle={loading ? 'Caricamento...' : richieste.length === 0
          ? 'Nessuna iscrizione da verificare'
          : `${richieste.length} ${richieste.length === 1 ? 'iscrizione in attesa' : 'iscrizioni in attesa'} di verifica`}
        actions={<Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />}
      >
        Richieste di iscrizione
      </PageTitle>

      <div className="flex items-start gap-3 bg-navy-50 border border-navy-100 rounded-sm px-4 py-3 mb-5 text-sm text-navy-800">
        <ClipboardCheck size={16} className="mt-0.5 shrink-0 text-navy-500" />
        <p>
          I commercialisti che si registrano restano <span className="font-semibold">disabilitati</span> finché
          non verifichi il numero di albo presso l'Ordine di appartenenza. Approvando la richiesta l'account viene
          attivato e l'utente riceve un'email; rifiutandola resta bloccato e puoi indicare il motivo.
        </p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 size={24} className="animate-spin text-navy-400" />
          <span className="ml-3 text-sm text-anthracite-400">Caricamento richieste...</span>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={richieste}
          emptyMessage="Nessuna richiesta di iscrizione in attesa"
        />
      )}

      <Modal
        isOpen={!!daRifiutare}
        onClose={() => setDaRifiutare(null)}
        title="Rifiuta richiesta di iscrizione"
        size="sm"
      >
        {daRifiutare && (
          <div className="space-y-4">
            <p className="text-sm text-anthracite-600">
              Stai rifiutando l'iscrizione di <span className="font-semibold text-navy-900">{daRifiutare.nome} {daRifiutare.cognome}</span>
              {daRifiutare.numeroAlbo && <> (albo <span className="font-mono">{daRifiutare.numeroAlbo}</span>)</>}.
              L'account resterà disabilitato e l'utente riceverà una notifica.
            </p>
            <Textarea
              id="motivazione-rifiuto"
              label="Motivazione (facoltativa)"
              placeholder="Es. Numero di albo non risultante presso l'Ordine indicato"
              value={motivazione}
              onChange={(e) => setMotivazione(e.target.value)}
              maxLength={500}
            />
            <div className="flex gap-3 justify-end">
              <Button variant="secondary" onClick={() => setDaRifiutare(null)}>Annulla</Button>
              <Button variant="danger" icon={UserX} loading={actionId === daRifiutare.id} onClick={handleRifiuta}>
                Rifiuta iscrizione
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
