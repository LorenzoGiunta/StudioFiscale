import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth, ROLE_ROUTES } from '../../contexts/AuthContext.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import authService from '../../services/AuthService.js';
import { Input, Button } from '../../components/ui/FormControls.jsx';
import { ArrowRight, ShieldAlert, AlertTriangle } from 'lucide-react';

/**
 * Pagina di accesso: raccoglie le credenziali, autentica l'utente e lo
 * reindirizza alla dashboard corrispondente al proprio ruolo.
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const { loginUser, getDashboardRoute } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  // Avviso persistente mostrato sopra il form (account disabilitato o credenziali errate)
  const [alert, setAlert] = useState(null);

  const validate = () => {
    const e = {};
    if (!form.email) e.email = 'Email obbligatoria';
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Email non valida';
    if (!form.password) e.password = 'Password obbligatoria';
    else if (form.password.length < 6) e.password = 'Minimo 6 caratteri';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setAlert(null);
    if (!validate()) return;
    setLoading(true);
    try {
      const data = await authService.login(form.email, form.password);
      // AuthResponse: { token, id, ruolo, email, nome, cognome }
      const userData = {
        id: data.id,
        nome: data.nome,
        cognome: data.cognome,
        email: data.email,
        ruolo: data.ruolo,
      };
      loginUser(userData, data.token);
      toast.success('Accesso effettuato con successo');

      // Use setTimeout to let context update
      setTimeout(() => {
        navigate(ROLE_ROUTES[data.ruolo] || '/dashboard/cliente');
      }, 100);
    } catch (err) {
      setAlert(buildAlert(err));
    } finally {
      setLoading(false);
    }
  };

  /**
   * Traduce l'errore di login in un avviso da mostrare nel form. Il backend
   * distingue l'account disabilitato (titolo "Account disabilitato") dalle
   * credenziali errate, restituendo per entrambi uno stato 401: qui i due casi
   * vengono presentati con stili e messaggi differenti.
   */
  const buildAlert = (err) => {
    const api = err.apiError || err.response?.data;
    const titolo = api?.error || '';

    if (titolo === 'Account disabilitato') {
      return {
        variant: 'disabled',
        title: 'Account disabilitato',
        message: api.message
          || "Il tuo account è stato disabilitato. Contatta l'amministratore.",
      };
    }

    return {
      variant: 'error',
      title: 'Accesso non riuscito',
      message: api?.message || err.message || 'Email o password non corretti. Riprova.',
    };
  };

  const set = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
    if (alert) setAlert(null);
  };

  return (
    <div className="min-h-screen flex">
      {/* Left - Branding panel */}
      <div className="hidden lg:flex lg:w-[45%] bg-navy-900 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-navy-900 via-navy-800 to-navy-950" />
        <div className="absolute top-0 right-0 w-96 h-96 bg-amber-500/5 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-navy-400/10 rounded-full blur-3xl" />
        
        <div className="relative z-10 flex flex-col justify-between p-12 w-full">
          <div>
            <div className="flex items-center gap-3 mb-16">
              <div className="w-10 h-10 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
                <span className="text-navy-900 font-extrabold text-base tracking-tight">SF</span>
              </div>
              <div>
                <h1 className="text-lg font-bold text-white tracking-tight leading-none">StudioFiscale</h1>
                <span className="text-[10px] text-navy-400 tracking-widest uppercase font-medium">Gestionale</span>
              </div>
            </div>

            <h2 className="text-3xl font-bold text-white leading-tight tracking-tight mb-4">
              Il gestionale<br />
              <span className="text-amber-400">per il tuo studio.</span>
            </h2>
            <p className="text-sm text-navy-300 leading-relaxed max-w-sm">
              Gestisci pratiche, documenti e comunicazioni con i tuoi clienti in un'unica piattaforma professionale.
            </p>
          </div>

          <div className="space-y-6">
            <div className="flex items-center gap-4">
              <div className="w-px h-12 bg-navy-700" />
              <div>
                <p className="text-sm text-white font-medium">Pratiche centralizzate</p>
                <p className="text-xs text-navy-400">Tutto lo storico in un unico luogo</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="w-px h-12 bg-navy-700" />
              <div>
                <p className="text-sm text-white font-medium">Comunicazione diretta</p>
                <p className="text-xs text-navy-400">Chat integrata con clienti e team</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="w-px h-12 bg-navy-700" />
              <div>
                <p className="text-sm text-white font-medium">Documenti sicuri</p>
                <p className="text-xs text-navy-400">Upload e revisione in tempo reale</p>
              </div>
            </div>
          </div>

        </div>
      </div>

      {/* Right - Login form */}
      <div className="flex-1 flex items-center justify-center p-6 bg-white">
        <div className="w-full max-w-sm">
          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-3 mb-10">
            <div className="w-9 h-9 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
              <span className="text-navy-900 font-extrabold text-sm">SF</span>
            </div>
            <h1 className="text-lg font-bold text-navy-900 tracking-tight">StudioFiscale</h1>
          </div>

          <div className="mb-8">
            <h2 className="text-xl font-bold text-navy-900 tracking-tight mb-1">Accedi al tuo account</h2>
            <p className="text-sm text-anthracite-400">Inserisci le tue credenziali per continuare</p>
          </div>

          {alert && <LoginAlert alert={alert} />}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              id="login-email"
              label="Email"
              type="email"
              placeholder="nome@studio.it"
              value={form.email}
              onChange={set('email')}
              error={errors.email}
            />
            <Input
              id="login-password"
              label="Password"
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={set('password')}
              error={errors.password}
            />


            <Button type="submit" variant="primary" size="lg" loading={loading} className="w-full mt-2" icon={ArrowRight}>
              Accedi
            </Button>
          </form>

          <p className="text-sm text-anthracite-400 text-center mt-6">
            Non hai un account?{' '}
            <Link to="/register" className="text-navy-700 font-medium hover:text-navy-900 transition-colors">
              Registrati
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

/**
 * Banner di avviso mostrato nel form di login. Lo stile e l'icona dipendono dal
 * tipo di esito: ambra/avviso per l'account disabilitato (azione richiesta lato
 * amministratore), rosso/errore per le credenziali non valide.
 */
function LoginAlert({ alert }) {
  const isDisabled = alert.variant === 'disabled';
  const Icon = isDisabled ? ShieldAlert : AlertTriangle;
  const styles = isDisabled
    ? 'border-amber-300 bg-amber-50 text-amber-800'
    : 'border-red-300 bg-red-50 text-red-800';
  const iconColor = isDisabled ? 'text-amber-600' : 'text-red-600';

  return (
    <div
      role="alert"
      className={`mb-5 flex items-start gap-3 rounded-sm border ${styles} px-4 py-3 animate-toast-in`}
    >
      <Icon className={`w-4 h-4 mt-0.5 shrink-0 ${iconColor}`} />
      <div>
        <p className="text-sm font-semibold leading-snug">{alert.title}</p>
        <p className="text-xs leading-snug mt-0.5 opacity-90">{alert.message}</p>
      </div>
    </div>
  );
}
