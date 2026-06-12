import { useState, useEffect, useCallback, useRef } from 'react';
import { PageTitle, SectionTitle } from '../../components/ui/Display.jsx';
import { Button, Select } from '../../components/ui/FormControls.jsx';
import { statoPraticaLabel } from '../../components/ui/Badge.jsx';
import { Upload, FileText, Download, Loader2, RefreshCw, CheckCircle2, RotateCcw } from 'lucide-react';
import { useToast } from '../../contexts/ToastContext.jsx';
import clienteService from '../../services/ClienteService.js';
import documentoService from '../../services/DocumentoService.js';

const TIPI_FILE = ['CUD', 'FATTURA', 'ESTRATTO_CONTO', 'DICHIARAZIONE', 'MODELLO_730', 'ALTRO'];

const FORMATI_ACCETTATI = '.pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg';

/**
 * Pagina dei documenti del cliente: consente di caricare nuovi documenti e nuove
 * versioni, scaricarli e consultarne lo stato di revisione.
 */
export default function ClienteDocumenti() {
  const toast = useToast();
  const fileInputRef = useRef(null);
  const versioneFileInputRef = useRef(null);

  const [docs, setDocs] = useState([]);
  const [pratiche, setPratiche] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);
  const [nuovaVersioneDocId, setNuovaVersioneDocId] = useState(null);
  const [uploadingVersione, setUploadingVersione] = useState(false);

  // Form upload
  const [selectedFile, setSelectedFile] = useState(null);
  const [nome, setNome] = useState('');
  const [tipoFile, setTipoFile] = useState('');
  const [praticaId, setPraticaId] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [dRes, pRes] = await Promise.allSettled([clienteService.getDocumenti(), clienteService.getPratiche()]);
      if (dRes.status === 'fulfilled') setDocs(dRes.value);
      if (pRes.status === 'fulfilled') setPratiche(pRes.value);
    } catch {
      toast.error('Errore nel caricamento dei documenti');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setSelectedFile(file);
    // Suggerisci nome dal file se il campo è vuoto
    if (!nome.trim()) setNome(file.name.replace(/\.[^/.]+$/, ''));
  };

  const handleSelectFile = () => {
    if (!tipoFile || !praticaId) {
      toast.warning('Seleziona prima il tipo di documento e la pratica');
      return;
    }
    fileInputRef.current?.click();
  };

  const handleUpload = async () => {
    if (!selectedFile) { toast.warning('Nessun file selezionato'); return; }
    if (!nome.trim())  { toast.warning('Inserisci un nome per il documento'); return; }
    if (!tipoFile)     { toast.warning('Seleziona il tipo di documento'); return; }
    if (!praticaId)    { toast.warning('Seleziona la pratica'); return; }

    const formData = new FormData();
    formData.append('file',      selectedFile);
    formData.append('nome',      nome.trim());
    formData.append('tipoFile',  tipoFile);
    formData.append('praticaId', praticaId);

    setUploading(true);
    try {
      await documentoService.upload(formData);
      toast.success(`"${nome.trim()}" caricato con successo`);
      // Reset form
      setSelectedFile(null);
      setNome('');
      setTipoFile('');
      setPraticaId('');
      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadData();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore durante il caricamento';
      toast.error(msg);
    } finally {
      setUploading(false);
    }
  };

  const handleNuovaVersioneClick = (docId) => {
    setNuovaVersioneDocId(docId);
    versioneFileInputRef.current?.click();
  };

  const handleNuovaVersioneFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file || !nuovaVersioneDocId) return;
    const formData = new FormData();
    formData.append('file', file);
    setUploadingVersione(true);
    try {
      await documentoService.nuovaVersione(nuovaVersioneDocId, formData);
      toast.success('Nuova versione caricata con successo');
      await loadData();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore nel caricamento della nuova versione';
      toast.error(msg);
    } finally {
      setUploadingVersione(false);
      setNuovaVersioneDocId(null);
      if (versioneFileInputRef.current) versioneFileInputRef.current.value = '';
    }
  };

  const handleDownload = async (doc) => {
    setDownloadingId(doc.id);
    try {
      await documentoService.download(doc.id, doc.nome);
    } catch {
      toast.error('Impossibile scaricare il file');
    } finally {
      setDownloadingId(null);
    }
  };

  const statoStyle = (s) => {
    if (s === 'APPROVATO') return 'text-emerald-600 bg-emerald-50';
    if (s === 'RIFIUTATO') return 'text-red-600 bg-red-50';
    return 'text-amber-600 bg-amber-50';
  };

  const statoLabel = (s) => {
    if (s === 'APPROVATO') return 'Approvato';
    if (s === 'RIFIUTATO') return 'Rifiutato';
    return 'In revisione';
  };

  const formatDate = (d) =>
    d ? new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';

  const formatSize = (bytes) => {
    if (!bytes) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle="Carica e gestisci i tuoi documenti"
        actions={<Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />}
      >
        Documenti
      </PageTitle>

      {/* Area Upload */}
      <div className="bg-white border border-anthracite-100 rounded-sm p-6 mb-6">
        <SectionTitle>Carica nuovo documento</SectionTitle>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
          {/* Tipo documento */}
          <div>
            <label className="block text-xs font-medium text-anthracite-500 mb-1.5">Tipo documento *</label>
            <select
              value={tipoFile}
              onChange={(e) => setTipoFile(e.target.value)}
              className="w-full px-3 py-2 text-sm bg-white border border-anthracite-200 rounded-sm outline-none focus:border-navy-400 transition-colors"
            >
              <option value="">Seleziona tipo...</option>
              {TIPI_FILE.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
            </select>
          </div>

          {/* Pratica */}
          <div>
            <label className="block text-xs font-medium text-anthracite-500 mb-1.5">Pratica collegata *</label>
            <select
              value={praticaId}
              onChange={(e) => setPraticaId(e.target.value)}
              className="w-full px-3 py-2 text-sm bg-white border border-anthracite-200 rounded-sm outline-none focus:border-navy-400 transition-colors"
            >
              <option value="">Seleziona pratica...</option>
              {pratiche.map(p => (
                <option key={p.id} value={p.id}>
                  {p.tipoPratica?.replace(/_/g, ' ')} — {statoPraticaLabel(p.stato)}
                </option>
              ))}
            </select>
          </div>

          {/* Nome documento */}
          <div>
            <label className="block text-xs font-medium text-anthracite-500 mb-1.5">Nome documento</label>
            <input
              type="text"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              placeholder="Es. CUD 2024 (auto dal file)"
              className="w-full px-3 py-2 text-sm bg-white border border-anthracite-200 rounded-sm outline-none focus:border-navy-400 transition-colors placeholder:text-anthracite-300"
            />
          </div>
        </div>

        {/* Zona drag & click per selezionare il file */}
        <div
          onClick={handleSelectFile}
          className={`border-2 border-dashed rounded-sm p-6 text-center cursor-pointer transition-colors ${
            selectedFile
              ? 'border-emerald-300 bg-emerald-50/30'
              : 'border-anthracite-200 hover:border-navy-300 hover:bg-anthracite-50/30'
          }`}
        >
          {selectedFile ? (
            <div className="flex items-center justify-center gap-3">
              <CheckCircle2 size={20} className="text-emerald-500" />
              <div className="text-left">
                <p className="text-sm font-medium text-navy-900">{selectedFile.name}</p>
                <p className="text-xs text-anthracite-400">{formatSize(selectedFile.size)}</p>
              </div>
            </div>
          ) : (
            <div>
              <Upload size={24} className="text-anthracite-300 mx-auto mb-2" />
              <p className="text-sm font-medium text-navy-900 mb-1">Clicca per selezionare il file</p>
              <p className="text-xs text-anthracite-400">PDF, DOC, DOCX, XLS, XLSX, immagini — Max 20MB</p>
            </div>
          )}
        </div>

        {/* Input file nascosto */}
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept={FORMATI_ACCETTATI}
          onChange={handleFileChange}
        />
        {/* Input file nascosto per nuova versione */}
        <input
          ref={versioneFileInputRef}
          type="file"
          className="hidden"
          accept={FORMATI_ACCETTATI}
          onChange={handleNuovaVersioneFileChange}
        />

        <div className="flex items-center justify-between mt-4">
          {selectedFile && (
            <button
              onClick={() => { setSelectedFile(null); if (fileInputRef.current) fileInputRef.current.value = ''; }}
              className="text-xs text-anthracite-400 hover:text-red-500 transition-colors"
            >
              ✕ Rimuovi file
            </button>
          )}
          <div className="ml-auto">
            <Button
              variant="primary"
              icon={Upload}
              onClick={handleUpload}
              loading={uploading}
              disabled={!selectedFile}
            >
              {uploading ? 'Caricamento...' : 'Carica documento'}
            </Button>
          </div>
        </div>
      </div>

      {/* Lista documenti */}
      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
        {loading ? (
          <div className="px-5 py-10 flex items-center justify-center">
            <Loader2 size={20} className="animate-spin text-navy-400 mr-2" />
            <span className="text-sm text-anthracite-400">Caricamento...</span>
          </div>
        ) : docs.length === 0 ? (
          <div className="px-5 py-10 text-center">
            <FileText size={32} className="text-anthracite-200 mx-auto mb-3" />
            <p className="text-sm text-anthracite-400">Nessun documento caricato</p>
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="bg-anthracite-50/70">
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Documento</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden sm:table-cell">Tipo</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Stato</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Dimensione</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100 hidden md:table-cell">Caricato</th>
                <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Download</th>
                <th className="px-5 py-3 border-b border-anthracite-100" />
              </tr>
            </thead>
            <tbody>
              {docs.map(d => (
                <tr key={d.id} className="border-b border-anthracite-50 last:border-b-0 hover:bg-anthracite-50/30 transition-colors">
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <FileText size={16} className="text-navy-400 shrink-0" />
                      <span className="text-sm font-medium text-navy-900 truncate max-w-[180px]">{d.nome}</span>
                    </div>
                  </td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-500 hidden sm:table-cell">{d.tipoFile}</td>
                  <td className="px-5 py-3.5">
                    <span className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold rounded-sm ${statoStyle(d.stato)}`}>
                      {statoLabel(d.stato)}
                    </span>
                  </td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatSize(d.dimensione)}</td>
                  <td className="px-5 py-3.5 text-sm text-anthracite-400 hidden md:table-cell">{formatDate(d.dataCaricamento)}</td>
                  <td className="px-5 py-3.5">
                    <button
                      onClick={() => handleDownload(d)}
                      disabled={downloadingId === d.id}
                      className="flex items-center gap-1.5 text-xs font-medium text-navy-600 hover:text-navy-900 hover:bg-navy-50 px-2.5 py-1.5 rounded-sm transition-colors disabled:opacity-40"
                      title="Scarica file"
                    >
                      {downloadingId === d.id
                        ? <Loader2 size={14} className="animate-spin" />
                        : <Download size={14} />
                      }
                      <span className="hidden sm:inline">Scarica</span>
                    </button>
                  </td>
                  <td className="px-5 py-3.5">
                    {d.stato === 'RIFIUTATO' && (
                      <button
                        onClick={() => handleNuovaVersioneClick(d.id)}
                        disabled={uploadingVersione && nuovaVersioneDocId === d.id}
                        className="flex items-center gap-1.5 text-xs font-medium text-amber-600 hover:text-amber-800 hover:bg-amber-50 px-2.5 py-1.5 rounded-sm transition-colors disabled:opacity-40"
                        title="Carica nuova versione"
                      >
                        {uploadingVersione && nuovaVersioneDocId === d.id
                          ? <Loader2 size={14} className="animate-spin" />
                          : <RotateCcw size={14} />
                        }
                        <span className="hidden sm:inline">Nuova versione</span>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
