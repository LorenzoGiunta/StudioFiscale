import { useState } from 'react';
import { Link } from 'react-router-dom';
import authService from '../../services/AuthService.js';
import { Input, Button } from '../../components/ui/FormControls.jsx';
import { ArrowLeft, ArrowRight, MailCheck, KeyRound } from 'lucide-react';

/**
 * Pagina pubblica di recupero password: l'utente indica la propria email e
 * riceve un link per scegliere una nuova password. Il messaggio di conferma è
 * identico sia che l'indirizzo esista sia che no, per non rivelare quali email
 * hanno un account.
 */
export default function PasswordDimenticataPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [inviata, setInviata] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email) { setError('Email obbligatoria'); return; }
    if (!/\S+@\S+\.\S+/.test(email)) { setError('Email non valida'); return; }
    setError('');
    setLoading(true);
    try {
      await authService.richiediResetPassword(email.trim());
      setInviata(true);
    } catch (err) {
      setError(err.apiError?.message || 'Impossibile inviare la richiesta. Riprova tra qualche istante.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 bg-white">
      <div className="w-full max-w-sm">
        <div className="flex items-center gap-3 mb-10">
          <div className="w-9 h-9 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
            <span className="text-navy-900 font-extrabold text-sm">SF</span>
          </div>
          <h1 className="text-lg font-bold text-navy-900 tracking-tight">StudioFiscale</h1>
        </div>

        {!inviata ? (
          <>
            <div className="mb-8">
              <div className="w-10 h-10 rounded-sm bg-navy-50 text-navy-500 flex items-center justify-center mb-4">
                <KeyRound size={20} strokeWidth={1.75} />
              </div>
              <h2 className="text-xl font-bold text-navy-900 tracking-tight mb-1">Password dimenticata?</h2>
              <p className="text-sm text-anthracite-400">
                Inserisci l'email del tuo account: ti invieremo un link per scegliere una nuova password.
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                id="reset-email"
                label="Email"
                type="email"
                placeholder="nome@studio.it"
                value={email}
                onChange={(e) => { setEmail(e.target.value); if (error) setError(''); }}
                error={error}
                autoFocus
              />
              <Button type="submit" variant="primary" size="lg" className="w-full" icon={ArrowRight} loading={loading}>
                Invia il link
              </Button>
            </form>
          </>
        ) : (
          <div className="space-y-5">
            <div className="flex items-start gap-3 bg-emerald-50 border border-emerald-200 rounded-sm px-4 py-4">
              <MailCheck size={20} className="mt-0.5 shrink-0 text-emerald-600" />
              <div className="text-sm text-emerald-900 leading-relaxed">
                <p className="font-semibold mb-1">Controlla la tua casella di posta</p>
                <p>
                  Se <span className="font-medium">{email}</span> è registrata, riceverai tra poco un'email con il link per
                  reimpostare la password. Il link vale <span className="font-medium">60 minuti</span>.
                </p>
              </div>
            </div>
            <p className="text-xs text-anthracite-400">
              Non è arrivata? Controlla nello spam oppure{' '}
              <button type="button" onClick={() => setInviata(false)} className="text-navy-700 font-medium hover:text-navy-900">
                riprova
              </button>.
            </p>
          </div>
        )}

        <p className="text-sm text-anthracite-400 text-center mt-8">
          <Link to="/login" className="inline-flex items-center gap-1.5 text-navy-700 font-medium hover:text-navy-900 transition-colors">
            <ArrowLeft size={14} /> Torna al login
          </Link>
        </p>
      </div>
    </div>
  );
}
