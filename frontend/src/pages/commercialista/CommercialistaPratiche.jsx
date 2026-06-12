import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import { Button, Input, Select } from '../../components/ui/FormControls.jsx';
import DataTable from '../../components/ui/DataTable.jsx';
import Modal from '../../components/ui/Modal.jsx';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../../contexts/ToastContext.jsx';
import praticaService from '../../services/PraticaService.js';
import commercialistaService from '../../services/CommercialistaService.js';
import { Plus, Filter, ArrowRight, Loader2, RefreshCw, UserCheck, Trash2 } from 'lucide-react';

/**
 * Pagina delle pratiche del commercialista: elenco con filtri, creazione di nuove
 * pratiche, assegnazione ai collaboratori, avanzamento di stato ed eliminazione.
 */
export default function CommercialistaPratiche() {
  const toast = useToast();
  const navigate = useNavigate();
  const [pratiche, setPratiche] = useState([]);
  const [clienti, setClienti] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [filtroStato, setFiltroStato] = useState('');
  const [filtroCliente, setFiltroCliente] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [newPratica, setNewPratica] = useState({ clienteId: '', tipoPratica: '', scadenza: '' });
  const [page, setPage] = useState(1);           // 1-based per il DataTable
  const [totalPages, setTotalPages] = useState(1);
  const [collaboratori, setCollaboratori] = useState([]);
  const [showAssegnaModal, setShowAssegnaModal] = useState(false);
  const [praticaSelezionata, setPraticaSelezionata] = useState(null);
  const [collaboratoreId, setCollaboratoreId] = useState('');
  const [assegnando, setAssegnando] = useState(false);

  const loadData = useCallback(async (pageNum = page) => {
    setLoading(true);
    try {
      // Pratiche paginate + clienti + collaboratori (endpoint dedicati al commercialista)
      const [pRes, cliRes, colRes] = await Promise.allSettled([
        praticaService.getAll(pageNum - 1, 20),
        commercialistaService.getClienti(),
        commercialistaService.getCollaboratori(),
      ]);
      if (pRes.status === 'fulfilled') {
        const pageData = pRes.value;
        setPratiche(pageData.content || []);
        setTotalPages(pageData.totalPages || 1);
      }
      if (cliRes.status === 'fulfilled') setClienti(cliRes.value || []);
      if (colRes.status === 'fulfilled') setCollaboratori(colRes.value || []);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento delle pratiche');
    } finally {
      setLoading(false);
    }
  }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(page); }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  // Filtro client-side sulla pagina corrente
  const filtered = pratiche.filter(p => {
    if (filtroStato && p.stato !== filtroStato) return false;
    if (filtroCliente && !p.nomeCliente?.toLowerCase().includes(filtroCliente.toLowerCase())) return false;
    return true;
  });

  const openAssegna = (pratica) => {
    setPraticaSelezionata(pratica);
    setCollaboratoreId('');
    setShowAssegnaModal(true);
  };

  const handleAssegna = async () => {
    if (!collaboratoreId) { toast.warning('Seleziona un collaboratore'); return; }
    setAssegnando(true);
    try {
      await praticaService.assegnaCollaboratore(praticaSelezionata.id, parseInt(collaboratoreId));
      toast.success('Collaboratore assegnato');
      setShowAssegnaModal(false);
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nell\'assegnazione');
    } finally {
      setAssegnando(false);
    }
  };

  const handleElimina = async (id) => {
    if (!window.confirm('Eliminare questa pratica? I documenti collegati rimarranno archiviati.')) return;
    try {
      await praticaService.elimina(id);
      toast.success('Pratica eliminata');
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nell\'eliminazione');
    }
  };

  const avanzaStato = async (id) => {
    try {
      await praticaService.avanzaStato(id);
      toast.success('Stato pratica avanzato');
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nell\'avanzamento dello stato');
    }
  };

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const columns = [
    { key: 'nomeCliente', label: 'Cliente', render: (v) => <span className="font-medium text-navy-900">{v || '—'}</span> },
    { key: 'tipoPratica', label: 'Tipo Pratica', render: (v) => v?.replace(/_/g, ' ') || '—' },
    { key: 'stato', label: 'Stato', render: (_, row) => <StatoBadge stato={row.stato} />, width: '180px' },
    { key: 'nomeCollaboratore', label: 'Collaboratore', render: (v) => v || '—' },
    { key: 'scadenza', label: 'Scadenza', render: (v) => formatDate(v) },
    { key: 'azioni', label: '', render: (_, row) => (
      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
        <Button variant="ghost" size="sm" icon={UserCheck} onClick={() => openAssegna(row)}>
          Assegna
        </Button>
        {row.stato !== 'COMPLETATA' && (
          <Button variant="ghost" size="sm" icon={ArrowRight} onClick={() => avanzaStato(row.id)}>
            Avanza
          </Button>
        )}
        <button
          onClick={() => handleElimina(row.id)}
          className="p-1.5 text-anthracite-300 hover:text-red-600 hover:bg-red-50 rounded-sm transition-colors"
          title="Elimina pratica"
        >
          <Trash2 size={14} />
        </button>
      </div>
    ), width: '230px' },
  ];

  const handleCreate = async () => {
    if (!newPratica.clienteId || !newPratica.tipoPratica || !newPratica.scadenza) {
      toast.warning('Compila tutti i campi obbligatori');
      return;
    }
    setSubmitting(true);
    try {
      await praticaService.create({
        clienteId: parseInt(newPratica.clienteId),
        tipoPratica: newPratica.tipoPratica,
        scadenza: newPratica.scadenza,
      });
      toast.success('Pratica creata con successo');
      setShowModal(false);
      setNewPratica({ clienteId: '', tipoPratica: '', scadenza: '' });
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || err.response?.data?.message || 'Errore nella creazione della pratica');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle="Gestisci tutte le pratiche dello studio"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="ghost" icon={RefreshCw} onClick={() => loadData(page)} loading={loading} />
            <Button icon={Plus} variant="accent" onClick={() => setShowModal(true)}>Nuova pratica</Button>
          </div>
        }
      >
        Gestione Pratiche
      </PageTitle>

      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <Filter size={15} className="text-anthracite-400" />
        <Select id="filter-stato" value={filtroStato} onChange={(e) => setFiltroStato(e.target.value)} className="w-48">
          <option value="">Tutti gli stati</option>
          <option value="BOZZA">Bozza</option>
          <option value="IN_LAVORAZIONE">In Lavorazione</option>
          <option value="IN_ATTESA_DOCUMENTI">In Attesa Documenti</option>
          <option value="COMPLETATA">Completata</option>
        </Select>
        <Input id="filter-cliente" placeholder="Cerca cliente..." value={filtroCliente} onChange={(e) => setFiltroCliente(e.target.value)} className="w-48" />
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 size={24} className="animate-spin text-navy-400" />
          <span className="ml-3 text-sm text-anthracite-400">Caricamento pratiche...</span>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={filtered}
          page={page}
          totalPages={totalPages}
          onPageChange={handlePageChange}
          onRowClick={(p) => navigate(`${p.id}`)}
        />
      )}

      {/* Modal assegna collaboratore */}
      <Modal isOpen={showAssegnaModal} onClose={() => setShowAssegnaModal(false)} title="Assegna collaboratore" size="sm">
        <div className="space-y-4">
          {praticaSelezionata && (
            <div className="p-3 bg-anthracite-50 rounded-sm text-sm">
              <p className="font-medium text-navy-900">{praticaSelezionata.tipoPratica?.replace(/_/g, ' ')}</p>
              <p className="text-xs text-anthracite-400 mt-0.5">Cliente: {praticaSelezionata.nomeCliente || '—'}</p>
            </div>
          )}
          <Select
            id="assegna-collaboratore"
            label="Collaboratore *"
            value={collaboratoreId}
            onChange={(e) => setCollaboratoreId(e.target.value)}
          >
            <option value="">Seleziona collaboratore</option>
            {collaboratori.map(c => (
              <option key={c.id} value={c.id}>{c.nome} {c.cognome}</option>
            ))}
          </Select>
          {collaboratori.length === 0 && (
            <p className="text-xs text-anthracite-400">Nessun collaboratore disponibile. Invitane uno dalla sezione Collaboratori.</p>
          )}
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setShowAssegnaModal(false)}>Annulla</Button>
            <Button variant="accent" icon={UserCheck} onClick={handleAssegna} loading={assegnando} disabled={collaboratori.length === 0}>
              Assegna
            </Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="Crea nuova pratica" size="md">
        <div className="space-y-4">
          <Select id="new-cliente" label="Cliente *" value={newPratica.clienteId} onChange={(e) => setNewPratica(p => ({ ...p, clienteId: e.target.value }))}>
            <option value="">Seleziona cliente</option>
            {clienti.map(c => (
              <option key={c.id} value={c.id}>{c.nome} {c.cognome}</option>
            ))}
          </Select>
          <Select id="new-tipo" label="Tipo Pratica *" value={newPratica.tipoPratica} onChange={(e) => setNewPratica(p => ({ ...p, tipoPratica: e.target.value }))}>
            <option value="">Seleziona tipo</option>
            <option value="DICHIARAZIONE_REDDITI">Dichiarazione Redditi</option>
            <option value="IVA">IVA</option>
            <option value="IMU">IMU</option>
          </Select>

          <Input id="new-scadenza" label="Data Scadenza *" type="date" value={newPratica.scadenza} onChange={(e) => setNewPratica(p => ({ ...p, scadenza: e.target.value }))} />
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>Annulla</Button>
            <Button variant="accent" icon={Plus} onClick={handleCreate} loading={submitting}>Crea pratica</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
