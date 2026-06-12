import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { Button, Input } from '../../components/ui/FormControls.jsx';
import DataTable from '../../components/ui/DataTable.jsx';
import Modal from '../../components/ui/Modal.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import invitoService from '../../services/InvitoService.js';
import {
  UserPlus,
  RefreshCw,
  Trash2,
  Send,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Mail,
} from 'lucide-react';

const STATO_INVITO = {
  PENDING: {
    label: 'In attesa',
    icon: Clock,
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    dot: 'bg-amber-400',
  },
  ACCEPTED: {
    label: 'Attivo',
    icon: CheckCircle2,
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    dot: 'bg-emerald-500',
  },
  DECLINED: {
    label: 'Rifiutato',
    icon: XCircle,
    bg: 'bg-red-50',
    text: 'text-red-600',
    dot: 'bg-red-400',
  },
  EXPIRED: {
    label: 'Scaduto',
    icon: AlertCircle,
    bg: 'bg-anthracite-100',
    text: 'text-anthracite-500',
    dot: 'bg-anthracite-400',
  },
};

function StatoInvitoBadge({ stato }) {
  const cfg = STATO_INVITO[stato] || STATO_INVITO.PENDING;
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wider rounded-sm ${cfg.bg} ${cfg.text}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot}`} />
      {cfg.label}
    </span>
  );
}

function KpiCard({ label, value, color, icon: Icon }) {
  return (
    <div className="bg-white border border-anthracite-100 rounded-sm p-4 flex items-center gap-4">
      <div className={`w-10 h-10 rounded-sm flex items-center justify-center ${color}`}>
        <Icon size={18} className="text-white" />
      </div>
      <div>
        <p className="text-2xl font-bold text-navy-900 leading-none">{value}</p>
        <p className="text-xs text-anthracite-400 mt-0.5 font-medium uppercase tracking-wide">{label}</p>
      </div>
    </div>
  );
}

/**
 * Pagina di gestione dei collaboratori: il commercialista invia inviti, ne
 * monitora lo stato (in attesa, accettati, rifiutati) e può revocarli.
 */
export default function CommercialistaCollaboratori() {
  const toast = useToast();

  const [inviti, setInviti] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [revocando, setRevocando] = useState(null); // id dell'invito in fase di revoca
  const [page, setPage] = useState(1);
  const [filtroStato, setFiltroStato] = useState('ALL');

  const loadInviti = useCallback(async () => {
    setLoading(true);
    try {
      const data = await invitoService.getMiei();
      setInviti(data);
    } catch {
      toast.error('Errore nel caricamento dei collaboratori');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadInviti(); }, [loadInviti]);

  const totali = inviti.length;
  const attivi = inviti.filter(i => i.stato === 'ACCEPTED').length;
  const pending = inviti.filter(i => i.stato === 'PENDING').length;
  const declined = inviti.filter(i => i.stato === 'DECLINED' || i.stato === 'EXPIRED').length;

  const filtered = filtroStato === 'ALL'
    ? inviti
    : inviti.filter(i => i.stato === filtroStato);

  const validateEmail = (v) => /^\S+@\S+\.\S+$/.test(v);

  const handleInvita = async () => {
    if (!validateEmail(email)) {
      setEmailError('Inserisci un\'email valida');
      return;
    }
    setEmailError('');
    setSubmitting(true);
    try {
      await invitoService.invita(email);
      toast.success(`Invito inviato a ${email}`);
      setShowModal(false);
      setEmail('');
      await loadInviti();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore nell\'invio dell\'invito';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRevoca = async (id, stato) => {
    const conferma = window.confirm(
      stato === 'ACCEPTED'
        ? 'Vuoi rimuovere questo collaboratore dallo studio? L\'associazione verrà terminata.'
        : 'Vuoi annullare questo invito?'
    );
    if (!conferma) return;
    setRevocando(id);
    try {
      await invitoService.revoca(id);
      toast.success(stato === 'ACCEPTED' ? 'Collaboratore rimosso' : 'Invito annullato');
      await loadInviti();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Errore nella revoca');
    } finally {
      setRevocando(null);
    }
  };

  const formatDate = (d) =>
    d ? new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';

  const columns = [
    {
      key: 'nomeCollaboratore',
      label: 'Collaboratore',
      render: (v, row) => (
        <div>
          <p className="font-medium text-navy-900">{v || '—'}</p>
          <p className="text-xs text-anthracite-400 flex items-center gap-1 mt-0.5">
            <Mail size={11} />
            {row.emailDestinatario}
          </p>
        </div>
      ),
    },
    {
      key: 'stato',
      label: 'Stato',
      render: (v) => <StatoInvitoBadge stato={v} />,
      width: '160px',
    },
    {
      key: 'creatoIl',
      label: 'Invitato il',
      render: (v) => <span className="text-sm text-anthracite-500">{formatDate(v)}</span>,
      width: '130px',
    },
    {
      key: 'scadeIl',
      label: 'Scade il',
      render: (v, row) =>
        row.stato === 'PENDING' ? (
          <span className="text-sm text-amber-600 font-medium">{formatDate(v)}</span>
        ) : (
          <span className="text-sm text-anthracite-400">—</span>
        ),
      width: '130px',
    },
    {
      key: 'azioni',
      label: '',
      render: (_, row) => {
        const canRevoke = row.stato === 'ACCEPTED' || row.stato === 'PENDING';
        if (!canRevoke) return null;
        return (
          <Button
            variant="ghost"
            size="sm"
            icon={Trash2}
            loading={revocando === row.id}
            onClick={() => handleRevoca(row.id, row.stato)}
            className="text-red-500 hover:text-red-700 hover:bg-red-50"
          >
            {row.stato === 'ACCEPTED' ? 'Rimuovi' : 'Annulla'}
          </Button>
        );
      },
      width: '130px',
    },
  ];

  return (
    <div className="animate-fade-in space-y-6">
      <PageTitle
        subtitle="Gestisci i collaboratori del tuo studio tramite inviti"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="ghost" icon={RefreshCw} onClick={loadInviti} loading={loading} />
            <Button icon={UserPlus} variant="accent" onClick={() => setShowModal(true)}>
              Invita collaboratore
            </Button>
          </div>
        }
      >
        Collaboratori
      </PageTitle>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCard label="Totale inviti" value={totali} color="bg-navy-700" icon={Mail} />
        <KpiCard label="Attivi" value={attivi} color="bg-emerald-500" icon={CheckCircle2} />
        <KpiCard label="In attesa" value={pending} color="bg-amber-500" icon={Clock} />
        <KpiCard label="Non accettati" value={declined} color="bg-anthracite-400" icon={XCircle} />
      </div>

      {/* Filtri per stato */}
      <div className="flex items-center gap-2 flex-wrap">
        {[
          { key: 'ALL', label: 'Tutti' },
          { key: 'ACCEPTED', label: '✓ Attivi' },
          { key: 'PENDING', label: '⏳ In attesa' },
          { key: 'DECLINED', label: '✗ Rifiutati' },
          { key: 'EXPIRED', label: '⌛ Scaduti' },
        ].map(f => (
          <button
            key={f.key}
            onClick={() => { setFiltroStato(f.key); setPage(1); }}
            className={`px-3 py-1.5 text-xs font-medium rounded-sm transition-all border ${
              filtroStato === f.key
                ? 'bg-navy-900 text-white border-navy-900'
                : 'bg-white text-anthracite-500 border-anthracite-200 hover:border-navy-400 hover:text-navy-800'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Tabella */}
      {loading ? (
        <div className="flex items-center justify-center py-16 text-anthracite-400">
          <RefreshCw size={20} className="animate-spin mr-3" />
          <span className="text-sm">Caricamento collaboratori...</span>
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white border border-anthracite-100 rounded-sm">
          <div className="w-14 h-14 bg-anthracite-50 rounded-sm flex items-center justify-center mb-4">
            <UserPlus size={24} className="text-anthracite-300" />
          </div>
          <p className="text-sm font-medium text-navy-900 mb-1">
            {filtroStato === 'ALL' ? 'Nessun collaboratore ancora' : 'Nessun risultato per questo filtro'}
          </p>
          <p className="text-xs text-anthracite-400 mb-4">
            {filtroStato === 'ALL' && 'Invia il primo invito per aggiungere collaboratori al tuo studio.'}
          </p>
          {filtroStato === 'ALL' && (
            <Button icon={UserPlus} variant="accent" onClick={() => setShowModal(true)}>
              Invita collaboratore
            </Button>
          )}
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={filtered}
          page={page}
          totalPages={Math.max(1, Math.ceil(filtered.length / 10))}
          onPageChange={setPage}
        />
      )}

      {/* Modal invito */}
      <Modal
        isOpen={showModal}
        onClose={() => { setShowModal(false); setEmail(''); setEmailError(''); }}
        title="Invita un collaboratore"
        size="sm"
      >
        <div className="space-y-5">
          {/* Illustrazione */}
          <div className="flex items-start gap-4 p-4 bg-navy-50 rounded-sm border border-navy-100">
            <div className="w-9 h-9 bg-navy-900 rounded-sm flex items-center justify-center shrink-0">
              <Send size={16} className="text-amber-400" />
            </div>
            <div>
              <p className="text-sm font-semibold text-navy-900 mb-0.5">Come funziona</p>
              <p className="text-xs text-anthracite-500 leading-relaxed">
                Il collaboratore riceverà un'email con un link per accettare o rifiutare
                l'invito. L'invito è valido per <strong>7 giorni</strong>.
              </p>
            </div>
          </div>

          {/* Campo email */}
          <div>
            <label className="block text-xs font-semibold text-navy-700 mb-1.5 uppercase tracking-wide">
              Email del collaboratore *
            </label>
            <input
              id="invito-email"
              type="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setEmailError(''); }}
              onKeyDown={(e) => e.key === 'Enter' && handleInvita()}
              placeholder="collaboratore@studio.it"
              className={`w-full px-3 py-2.5 text-sm border rounded-sm outline-none transition-all
                bg-white text-navy-900 placeholder-anthracite-300
                focus:ring-2 focus:ring-navy-300 focus:border-navy-500
                ${emailError ? 'border-red-400 focus:ring-red-200' : 'border-anthracite-200'}`}
            />
            {emailError && (
              <p className="mt-1.5 text-xs text-red-500 flex items-center gap-1">
                <AlertCircle size={11} />
                {emailError}
              </p>
            )}
          </div>

          {/* Azioni */}
          <div className="flex justify-end gap-3 pt-1">
            <Button
              variant="secondary"
              onClick={() => { setShowModal(false); setEmail(''); setEmailError(''); }}
            >
              Annulla
            </Button>
            <Button
              variant="accent"
              icon={Send}
              onClick={handleInvita}
              loading={submitting}
            >
              Invia invito
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
