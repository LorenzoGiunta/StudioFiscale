import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { Button } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import invitoService from '../../services/InvitoService.js';
import {
  Building2,
  Clock,
  CheckCircle2,
  XCircle,
  RefreshCw,
  UserCheck,
  Bell,
  CalendarX,
  Mail,
  ArrowRight,
} from 'lucide-react';

const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—';

const daysLeft = (scadeIl) => {
  if (!scadeIl) return null;
  const diff = new Date(scadeIl) - new Date();
  return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
};

function StudioCard({ invito }) {
  return (
    <div className="bg-white border border-anthracite-100 rounded-sm p-5 flex items-center gap-5 hover:border-navy-200 hover:shadow-sm transition-all group">
      {/* Avatar studio */}
      <div className="w-12 h-12 bg-gradient-to-br from-navy-700 to-navy-900 rounded-sm flex items-center justify-center shrink-0">
        <Building2 size={20} className="text-amber-400" />
      </div>

      <div className="flex-1 min-w-0">
        <p className="font-semibold text-navy-900 truncate">{invito.nomeCommercialista}</p>
        <p className="text-xs text-anthracite-400 mt-0.5 flex items-center gap-1.5">
          <Mail size={11} />
          {invito.emailDestinatario}
        </p>
        <p className="text-xs text-anthracite-400 mt-0.5">
          Albo n° {invito.studioCommercialista} · associato dal {formatDate(invito.creatoIl)}
        </p>
      </div>

      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wider rounded-sm bg-emerald-50 text-emerald-700 shrink-0">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
        Attivo
      </span>
    </div>
  );
}

function InvitoPendingCard({ invito, onAccetta, accepting }) {
  const giorni = daysLeft(invito.scadeIl);

  return (
    <div className="bg-white border-2 border-amber-200 rounded-sm p-5 relative overflow-hidden">
      {/* Striscia decorativa */}
      <div className="absolute top-0 left-0 w-1 h-full bg-amber-400 rounded-l-sm" />

      <div className="pl-3">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-4">
            <div className="w-11 h-11 bg-amber-50 rounded-sm flex items-center justify-center shrink-0">
              <Bell size={18} className="text-amber-500" />
            </div>
            <div>
              <p className="font-semibold text-navy-900">
                Invito da <span className="text-amber-700">{invito.nomeCommercialista}</span>
              </p>
              <p className="text-xs text-anthracite-400 mt-0.5">
                Albo n° {invito.studioCommercialista}
              </p>
            </div>
          </div>

          {/* Badge scadenza */}
          <div className="flex items-center gap-1.5 text-xs text-amber-600 font-medium bg-amber-50 px-2.5 py-1.5 rounded-sm border border-amber-200">
            <CalendarX size={12} />
            {giorni === 0
              ? 'Scade oggi!'
              : giorni === 1
              ? 'Scade domani'
              : `Scade tra ${giorni} giorni`}
          </div>
        </div>

        <p className="text-xs text-anthracite-400 mt-3">
          Hai ricevuto questo invito il <strong>{formatDate(invito.creatoIl)}</strong>.
          Accettando diventerai collaboratore di questo studio.
        </p>

        <div className="flex items-center gap-2 mt-4">
          <Button
            variant="accent"
            size="sm"
            icon={UserCheck}
            loading={accepting}
            onClick={() => onAccetta(invito.token)}
          >
            Accetta invito
          </Button>
          <a
            href={`/invito/${invito.token}/rifiuta`}
            className="px-3 py-1.5 text-xs font-medium text-anthracite-500 hover:text-red-600 border border-anthracite-200 hover:border-red-300 rounded-sm transition-all flex items-center gap-1.5"
          >
            <XCircle size={13} />
            Rifiuta
          </a>
        </div>
      </div>
    </div>
  );
}

/**
 * Pagina degli studi del collaboratore: mostra gli inviti ricevuti e le
 * collaborazioni attive, con la possibilità di accettare i nuovi inviti.
 */
export default function CollaboratoreMieiStudi() {
  const toast = useToast();

  const [studi, setStudi] = useState([]);          // inviti ACCEPTED
  const [pending, setPending] = useState([]);       // inviti PENDING
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(null); // token in fase di accettazione

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [pendingRes, accettatiRes] = await Promise.allSettled([
        invitoService.getPending(),
        invitoService.getAccettati(),
      ]);

      if (pendingRes.status === 'fulfilled') {
        setPending(pendingRes.value);
      }
      if (accettatiRes.status === 'fulfilled') {
        setStudi(accettatiRes.value);
      }
    } catch {
      toast.error('Errore nel caricamento degli studi');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const handleAccetta = async (token) => {
    setAccepting(token);
    try {
      await invitoService.accetta(token);
      toast.success('Invito accettato! Sei ora associato allo studio.');
      await loadData();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore nell\'accettazione';
      toast.error(msg);
    } finally {
      setAccepting(null);
    }
  };

  return (
    <div className="animate-fade-in space-y-8">
      <PageTitle
        subtitle="Gli studi a cui sei associato e gli inviti in attesa"
        actions={
          <Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />
        }
      >
        I miei Studi
      </PageTitle>

      {loading ? (
        <div className="flex items-center justify-center py-16 text-anthracite-400">
          <RefreshCw size={20} className="animate-spin mr-3" />
          <span className="text-sm">Caricamento...</span>
        </div>
      ) : (
        <>
          {/* Inviti in attesa */}
          {pending.length > 0 && (
            <section>
              <div className="flex items-center gap-2 mb-3">
                <Bell size={15} className="text-amber-500" />
                <h2 className="text-sm font-semibold text-navy-900 uppercase tracking-wide">
                  Inviti in attesa
                </h2>
                <span className="ml-1 px-2 py-0.5 text-xs font-bold bg-amber-500 text-white rounded-full">
                  {pending.length}
                </span>
              </div>
              <div className="space-y-3">
                {pending.map(invito => (
                  <InvitoPendingCard
                    key={invito.id}
                    invito={invito}
                    onAccetta={handleAccetta}
                    accepting={accepting === invito.token}
                  />
                ))}
              </div>
            </section>
          )}

          {/* Studi attivi */}
          <section>
            <div className="flex items-center gap-2 mb-3">
              <Building2 size={15} className="text-navy-500" />
              <h2 className="text-sm font-semibold text-navy-900 uppercase tracking-wide">
                Studi associati
              </h2>
              <span className="ml-1 px-2 py-0.5 text-xs font-semibold bg-emerald-100 text-emerald-700 rounded-full">
                {studi.length}
              </span>
            </div>

            {studi.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 bg-white border border-dashed border-anthracite-200 rounded-sm">
                <div className="w-14 h-14 bg-anthracite-50 rounded-sm flex items-center justify-center mb-4">
                  <Building2 size={24} className="text-anthracite-300" />
                </div>
                <p className="text-sm font-medium text-navy-900 mb-1">
                  Nessuno studio associato
                </p>
                <p className="text-xs text-anthracite-400 text-center max-w-xs">
                  Non sei ancora associato a nessuno studio. Attendi un invito da un Commercialista.
                </p>
              </div>
            ) : (
              <div className="space-y-2">
                {studi.map(invito => (
                  <StudioCard key={invito.id} invito={invito} />
                ))}
              </div>
            )}
          </section>

          {/* Stato vuoto globale */}
          {studi.length === 0 && pending.length === 0 && (
            <div className="bg-navy-50 border border-navy-100 rounded-sm p-5 flex items-start gap-4 mt-4">
              <CheckCircle2 size={18} className="text-navy-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-sm font-semibold text-navy-800 mb-1">Come funziona?</p>
                <p className="text-xs text-navy-500 leading-relaxed">
                  In StudioFiscale, sono i Commercialisti ad invitare i Collaboratori.
                  Non puoi auto-associarti: attendi che un Commercialista ti invii un invito
                  via email, poi accettalo da questa pagina o dal link nell'email.
                </p>
                <p className="inline-flex items-center gap-1 mt-2 text-xs text-navy-500">
                  <ArrowRight size={12} />
                  Attendi un invito via email da un commercialista
                </p>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
