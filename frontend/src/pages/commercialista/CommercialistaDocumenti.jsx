import { useState, useEffect, useCallback } from 'react';
import { PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { Button, Input, Select } from '../../components/ui/FormControls.jsx';
import Modal from '../../components/ui/Modal.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import { FileText, CheckCircle, XCircle, Loader2, RefreshCw, Trash2, UserCheck } from 'lucide-react';
import commercialistaService from '../../services/CommercialistaService.js';
import documentoService from '../../services/DocumentoService.js';

/**
 * Pagina dei documenti dello studio: consente al commercialista di consultarli,
 * assegnarne la revisione a un collaboratore e gestirne lo stato.
 */
export default function CommercialistaDocumenti() {
  const toast = useToast();
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState(null);
  const [showRifiutaModal, setShowRifiutaModal] = useState(false);
  const [docDaRifiutare, setDocDaRifiutare] = useState(null);
  const [motivazione, setMotivazione] = useState('');
  const [showRevisoreModal, setShowRevisoreModal] = useState(false);
  const [docPerRevisore, setDocPerRevisore] = useState(null);
  const [collaboratori, setCollaboratori] = useState([]);
  const [revisoreId, setRevisoreId] = useState('');
  const [assegnandoRevisore, setAssegnandoRevisore] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [docsData, colData] = await Promise.allSettled([
        commercialistaService.getDocumenti(),
        commercialistaService.getCollaboratori(),
      ]);
      if (docsData.status === 'fulfilled') setDocs(docsData.value);
      if (colData.status === 'fulfilled') setCollaboratori(colData.value || []);
    } catch {
      toast.error('Errore nel caricamento dei documenti');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const handleApprova = async (id) => {
    setActionId(id);
    try {
      await documentoService.approva(id);
      toast.success('Documento approvato');
      await loadData();
    } catch {
      toast.error('Errore nell\'approvazione del documento');
    } finally {
      setActionId(null);
    }
  };

  const handleElimina = async (id) => {
    if (!window.confirm('Eliminare questo documento? Il file fisico rimarrà archiviato.')) return;
    try {
      await documentoService.elimina(id);
      toast.success('Documento eliminato');
      await loadData();
    } catch {
      toast.error('Errore nell\'eliminazione del documento');
    }
  };

  const openRevisore = (doc) => {
    setDocPerRevisore(doc);
    setRevisoreId('');
    setShowRevisoreModal(true);
  };

  const handleAssegnaRevisore = async () => {
    if (!revisoreId) { toast.warning('Seleziona un collaboratore'); return; }
    setAssegnandoRevisore(true);
    try {
      await documentoService.assegnaRevisore(docPerRevisore.id, parseInt(revisoreId));
      toast.success('Revisore assegnato');
      setShowRevisoreModal(false);
      await loadData();
    } catch {
      toast.error('Errore nell\'assegnazione del revisore');
    } finally {
      setAssegnandoRevisore(false);
    }
  };

  const openRifiuta = (doc) => {
    setDocDaRifiutare(doc);
    setMotivazione('');
    setShowRifiutaModal(true);
  };

  const handleRifiuta = async () => {
    if (!motivazione.trim()) { toast.warning('Inserisci una motivazione'); return; }
    setActionId(docDaRifiutare.id);
    setShowRifiutaModal(false);
    try {
      await documentoService.rifiuta(docDaRifiutare.id, motivazione.trim());
      toast.success('Documento rifiutato');
      await loadData();
    } catch {
      toast.error('Errore nel rifiuto del documento');
    } finally {
      setActionId(null);
      setDocDaRifiutare(null);
    }
  };

  const formatDate = (d) => {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const statoStyle = (s) => {
    if (s === 'APPROVATO') return 'text-emerald-600 bg-emerald-50';
    if (s === 'RIFIUTATO') return 'text-red-600 bg-red-50';
    return 'text-amber-600 bg-amber-50';
  };

  const statoLabel = (s) => {
    if (s === 'APPROVATO') return 'Approvato';
    if (s === 'RIFIUTATO') return 'Rifiutato';
    return 'Da revisionare';
  };

  const daRevisionare = docs.filter(d => d.stato === 'IN_REVISIONE' || !d.stato);
  const revisionati = docs.filter(d => d.stato === 'APPROVATO' || d.stato === 'RIFIUTATO');

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento documenti...</span>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle={`${daRevisionare.length} documenti in attesa di revisione`}
        actions={<Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />}
      >
        Revisione Documenti
      </PageTitle>

      {/* Da revisionare */}
      {daRevisionare.length > 0 && (
        <div className="mb-8">
          <SectionTitle>Da revisionare</SectionTitle>
          <div className="grid gap-3">
            {daRevisionare.map(d => (
              <div key={d.id} className="bg-white border border-amber-100 rounded-sm p-5 flex items-center gap-4 hover:shadow-sm transition-all">
                <div className="w-10 h-10 rounded-sm bg-amber-50 flex items-center justify-center shrink-0">
                  <FileText size={18} className="text-amber-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-navy-900">{d.nome}</p>
                  <p className="text-xs text-anthracite-400 mt-0.5">
                    {d.tipoFile} · {formatDate(d.dataCaricamento)}
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Button variant="ghost" size="sm" icon={UserCheck} onClick={() => openRevisore(d)}>
                    Revisore
                  </Button>
                  <Button
                    variant="secondary" size="sm" icon={XCircle}
                    loading={actionId === d.id}
                    onClick={() => openRifiuta(d)}
                  >
                    Rifiuta
                  </Button>
                  <Button
                    variant="primary" size="sm" icon={CheckCircle}
                    loading={actionId === d.id}
                    onClick={() => handleApprova(d.id)}
                  >
                    Approva
                  </Button>
                  <button
                    onClick={() => handleElimina(d.id)}
                    className="p-1.5 text-anthracite-300 hover:text-red-600 hover:bg-red-50 rounded-sm transition-colors"
                    title="Elimina documento"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Storico */}
      <SectionTitle>Storico revisioni</SectionTitle>
      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
        {revisionati.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-anthracite-400">Nessun documento revisionato</div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="bg-anthracite-50/70">
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Documento</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Tipo</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Data</th>
                <th className="px-5 py-3 border-b border-anthracite-100" />
              </tr>
            </thead>
            <tbody>
              {revisionati.map(d => (
                <tr key={d.id} className="border-b border-anthracite-50 last:border-b-0">
                  <td className="px-5 py-3.5 text-sm font-medium text-navy-900">{d.nome}</td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-500">{d.tipoFile}</td>
                  <td className="px-5 py-3.5">
                    <span className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold rounded-sm ${statoStyle(d.stato)}`}>
                      {statoLabel(d.stato)}
                    </span>
                  </td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatDate(d.dataCaricamento)}</td>
                  <td className="px-5 py-3.5">
                    <button
                      onClick={() => handleElimina(d.id)}
                      className="p-1.5 text-anthracite-300 hover:text-red-600 hover:bg-red-50 rounded-sm transition-colors"
                      title="Elimina documento"
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal rifiuto documento */}
      <Modal
        isOpen={showRifiutaModal}
        onClose={() => setShowRifiutaModal(false)}
        title="Rifiuta documento"
        size="sm"
      >
        <div className="space-y-4">
          {docDaRifiutare && (
            <div className="p-3 bg-anthracite-50 rounded-sm text-sm">
              <p className="font-medium text-navy-900">{docDaRifiutare.nome}</p>
              <p className="text-xs text-anthracite-400 mt-0.5">{docDaRifiutare.tipoFile}</p>
            </div>
          )}
          <Input
            id="motivazione-rifiuto"
            label="Motivazione del rifiuto *"
            placeholder="Es. Documento illeggibile, dati mancanti..."
            value={motivazione}
            onChange={(e) => setMotivazione(e.target.value)}
          />
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="secondary" onClick={() => setShowRifiutaModal(false)}>Annulla</Button>
            <Button variant="primary" icon={XCircle} onClick={handleRifiuta}>
              Conferma rifiuto
            </Button>
          </div>
        </div>
      </Modal>

      {/* Modal assegna revisore */}
      <Modal
        isOpen={showRevisoreModal}
        onClose={() => setShowRevisoreModal(false)}
        title="Assegna revisore"
        size="sm"
      >
        <div className="space-y-4">
          {docPerRevisore && (
            <div className="p-3 bg-anthracite-50 rounded-sm text-sm">
              <p className="font-medium text-navy-900">{docPerRevisore.nome}</p>
              <p className="text-xs text-anthracite-400 mt-0.5">{docPerRevisore.tipoFile}</p>
            </div>
          )}
          <Select
            id="assegna-revisore"
            label="Collaboratore *"
            value={revisoreId}
            onChange={(e) => setRevisoreId(e.target.value)}
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
            <Button variant="secondary" onClick={() => setShowRevisoreModal(false)}>Annulla</Button>
            <Button
              variant="primary"
              icon={UserCheck}
              onClick={handleAssegnaRevisore}
              loading={assegnandoRevisore}
              disabled={collaboratori.length === 0}
            >
              Assegna revisore
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
