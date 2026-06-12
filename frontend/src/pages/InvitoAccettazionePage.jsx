import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext.jsx';
import { useToast } from '../contexts/ToastContext.jsx';
import invitoService from '../services/InvitoService.js';
import { CheckCircle2, XCircle, Loader2, Building2, ArrowRight, LogIn } from 'lucide-react';

/**
 * Pagina pubblica raggiungibile dal link inviato via email, che gestisce sia
 * l'accettazione sia il rifiuto di un invito di collaborazione.
 *
 * L'azione è determinata dal percorso: l'accettazione richiede l'autenticazione
 * del collaboratore, mentre il rifiuto è consentito anche senza login.
 */
export default function InvitoAccettazionePage() {
  const { token, azione } = useParams(); // azione = 'accetta' | 'rifiuta'
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const toast = useToast();

  const isAccetta = azione === 'accetta';

  // Stati UI
  const [status, setStatus] = useState('idle'); // idle | loading | success | error | needsLogin
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (!token) return;

    const esegui = async () => {
      setStatus('loading');

      try {
        if (!isAccetta) {
          // RIFIUTO — pubblico, nessuna autenticazione richiesta
          await invitoService.rifiutaPubblico(token);
          setStatus('success');
          return;
        }

        // ACCETTAZIONE — richiede autenticazione COLLABORATORE
        if (!isAuthenticated) {
          setStatus('needsLogin');
          return;
        }

        if (user?.ruolo !== 'COLLABORATORE') {
          setErrorMsg('Solo un Collaboratore può accettare un invito.');
          setStatus('error');
          return;
        }

        await invitoService.accetta(token);
        setStatus('success');

        // Dopo 2s naviga alla dashboard
        setTimeout(() => navigate('/dashboard/collaboratore'), 2000);
      } catch (err) {
        const msg =
          err.response?.data?.message ||
          err.message ||
          'Si è verificato un errore. Il link potrebbe essere scaduto o non valido.';
        setErrorMsg(msg);
        setStatus('error');
      }
    };

    esegui();
  }, [token, azione]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen bg-gradient-to-br from-navy-950 via-navy-900 to-navy-800 flex items-center justify-center p-6">
      {/* Blob decorativo */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-navy-400/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 w-full max-w-md">
        {/* Header brand */}
        <div className="flex items-center gap-3 mb-8">
          <div className="w-9 h-9 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
            <span className="text-navy-900 font-extrabold text-sm">SF</span>
          </div>
          <div>
            <h1 className="text-base font-bold text-white tracking-tight leading-none">StudioFiscale</h1>
            <span className="text-[10px] text-navy-300 tracking-widest uppercase font-medium">Gestionale</span>
          </div>
        </div>

        {/* Card principale */}
        <div className="bg-white rounded-sm shadow-2xl overflow-hidden">
          {/* Top accent bar */}
          <div className={`h-1.5 w-full ${isAccetta ? 'bg-gradient-to-r from-navy-700 to-amber-500' : 'bg-gradient-to-r from-red-400 to-red-600'}`} />

          <div className="p-8">
            {/* LOADING */}
            {status === 'loading' && (
              <div className="flex flex-col items-center text-center py-6">
                <div className="w-16 h-16 bg-navy-50 rounded-sm flex items-center justify-center mb-5">
                  <Loader2 size={28} className="text-navy-600 animate-spin" />
                </div>
                <h2 className="text-lg font-bold text-navy-900 mb-2">
                  {isAccetta ? 'Accettazione in corso...' : 'Rifiuto in corso...'}
                </h2>
                <p className="text-sm text-anthracite-400">Attendere prego.</p>
              </div>
            )}

            {/* NEEDS LOGIN */}
            {status === 'needsLogin' && (
              <div className="flex flex-col items-center text-center py-4">
                <div className="w-16 h-16 bg-navy-50 rounded-sm flex items-center justify-center mb-5">
                  <Building2 size={28} className="text-navy-600" />
                </div>
                <h2 className="text-lg font-bold text-navy-900 mb-2">Accesso richiesto</h2>
                <p className="text-sm text-anthracite-500 leading-relaxed mb-6">
                  Per accettare l'invito devi accedere con il tuo account Collaboratore.
                  Dopo il login verrai reindirizzato automaticamente.
                </p>

                <div className="flex flex-col gap-3 w-full">
                  <Link
                    to={`/login?redirect=/invito/${token}/accetta`}
                    className="flex items-center justify-center gap-2 w-full px-4 py-3 bg-navy-900 text-white text-sm font-semibold rounded-sm hover:bg-navy-800 transition-colors"
                  >
                    <LogIn size={16} />
                    Accedi al tuo account
                  </Link>
                  <Link
                    to={`/register?redirect=/invito/${token}/accetta`}
                    className="flex items-center justify-center gap-2 w-full px-4 py-3 bg-anthracite-50 text-navy-700 text-sm font-medium rounded-sm border border-anthracite-200 hover:border-navy-300 transition-colors"
                  >
                    Non hai un account? Registrati
                    <ArrowRight size={14} />
                  </Link>
                </div>
              </div>
            )}

            {/* SUCCESS */}
            {status === 'success' && (
              <div className="flex flex-col items-center text-center py-6">
                <div className={`w-16 h-16 rounded-sm flex items-center justify-center mb-5 ${isAccetta ? 'bg-emerald-50' : 'bg-red-50'}`}>
                  {isAccetta
                    ? <CheckCircle2 size={32} className="text-emerald-500" />
                    : <XCircle size={32} className="text-red-400" />
                  }
                </div>
                <h2 className="text-lg font-bold text-navy-900 mb-2">
                  {isAccetta ? 'Invito accettato!' : 'Invito rifiutato'}
                </h2>
                <p className="text-sm text-anthracite-500 leading-relaxed mb-6">
                  {isAccetta
                    ? 'Sei ora associato allo studio. Verrai reindirizzato alla dashboard tra pochi secondi.'
                    : 'Hai rifiutato l\'invito. Puoi chiudere questa pagina.'}
                </p>

                {isAccetta ? (
                  <Link
                    to="/dashboard/collaboratore"
                    className="flex items-center gap-2 px-5 py-2.5 bg-navy-900 text-white text-sm font-semibold rounded-sm hover:bg-navy-800 transition-colors"
                  >
                    Vai alla dashboard <ArrowRight size={14} />
                  </Link>
                ) : (
                  <Link
                    to="/login"
                    className="text-sm text-navy-600 font-medium hover:text-navy-900 transition-colors underline-offset-2 hover:underline"
                  >
                    Torna al login
                  </Link>
                )}
              </div>
            )}

            {/* ERROR */}
            {status === 'error' && (
              <div className="flex flex-col items-center text-center py-6">
                <div className="w-16 h-16 bg-red-50 rounded-sm flex items-center justify-center mb-5">
                  <XCircle size={32} className="text-red-400" />
                </div>
                <h2 className="text-lg font-bold text-navy-900 mb-2">Operazione non riuscita</h2>
                <p className="text-sm text-anthracite-500 leading-relaxed mb-1">{errorMsg}</p>
                <p className="text-xs text-anthracite-400 mb-6">
                  Il link potrebbe essere scaduto, già utilizzato o non valido.
                </p>
                <Link
                  to="/login"
                  className="text-sm text-navy-600 font-medium hover:text-navy-900 transition-colors underline-offset-2 hover:underline"
                >
                  Torna al login
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-[11px] text-navy-500 mt-6">
          © 2024 StudioFiscale. Hai bisogno di aiuto?{' '}
          <a href="mailto:support@studiofiscale.it" className="text-navy-300 hover:text-amber-400 transition-colors">
            Contattaci
          </a>
        </p>
      </div>
    </div>
  );
}
