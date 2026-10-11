import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import authService from '../../services/AuthService.js';
import { Input, Button } from '../../components/ui/FormControls.jsx';
import { ArrowRight, CheckCircle2, AlertTriangle, LockKeyhole } from 'lucide-react';

// Stessi requisiti della registrazione (e del backend)
const PASSWORD_REGEX = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\-]).{8,}$/;

/**
 * Pagina pubblica raggiunta dal link ricevuto via email: l'utente sceglie la
 * nuova password, che viene salvata solo se il token nel percorso è ancora
 * valido (non usato e non scaduto). In caso contrario viene invitato a
 * richiedere un nuovo link.
 */
export default function ResetPasswordPage() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState({ password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('form');   // form | success | invalid
  const [errorMsg, setErrorMsg] = useState('');

  const set = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
  };

  const validate = () => {
    const e = {};
    if (!form.password) e.password = 'Password obbligatoria';
    else if (!PASSWORD_REGEX.test(form.password))
      e.password = 'Min. 8 caratteri con maiuscola, minuscola, numero e carattere speciale (@#$%^&+=!_-)';
    if (form.password !== form.confirm) e.confirm = 'Le password non corrispondono';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    try {
      await authService.resetPassword(token, form.password);
      setStatus('success');
    } catch (err) {
      const api = err.apiError || err.response?.data;
      // Il backend distingue il link non valido/scaduto (400 "Link non valido")
      // dagli altri errori: nel primo caso il form non serve più.
      if (api?.error === 'Link non valido') {
        setErrorMsg(api.message);
        setStatus('invalid');
      } else {
        setErrors({ password: api?.message || 'Errore durante il salvataggio. Riprova.' });
      }
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

        {status === 'form' && (
          <>
            <div className="mb-8">
              <div className="w-10 h-10 rounded-sm bg-navy-50 text-navy-500 flex items-center justify-center mb-4">
                <LockKeyhole size={20} strokeWidth={1.75} />
              </div>
              <h2 className="text-xl font-bold text-navy-900 tracking-tight mb-1">Scegli una nuova password</h2>
              <p className="text-sm text-anthracite-400">Dovrà rispettare gli stessi requisiti della registrazione.</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                id="reset-password"
                label="Nuova password"
                type="password"
                placeholder="Minimo 8 caratteri"
                value={form.password}
                onChange={set('password')}
                error={errors.password}
                autoFocus
              />
              <Input
                id="reset-confirm"
                label="Conferma password"
                type="password"
                placeholder="Ripeti la password"
                value={form.confirm}
                onChange={set('confirm')}
                error={errors.confirm}
              />
              <Button type="submit" variant="primary" size="lg" className="w-full" icon={ArrowRight} loading={loading}>
                Salva la nuova password
              </Button>
            </form>
          </>
        )}

        {status === 'success' && (
          <div className="space-y-5">
            <div className="flex items-start gap-3 bg-emerald-50 border border-emerald-200 rounded-sm px-4 py-4">
              <CheckCircle2 size={20} className="mt-0.5 shrink-0 text-emerald-600" />
              <div className="text-sm text-emerald-900 leading-relaxed">
                <p className="font-semibold mb-1">Password aggiornata</p>
                <p>Da ora puoi accedere con la nuova password. Ti abbiamo inviato un'email di conferma.</p>
              </div>
            </div>
            <Button variant="primary" size="lg" className="w-full" icon={ArrowRight} onClick={() => navigate('/login')}>
              Vai al login
            </Button>
          </div>
        )}

        {status === 'invalid' && (
          <div className="space-y-5">
            <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-sm px-4 py-4">
              <AlertTriangle size={20} className="mt-0.5 shrink-0 text-red-600" />
              <div className="text-sm text-red-900 leading-relaxed">
                <p className="font-semibold mb-1">Link non valido o scaduto</p>
                <p>{errorMsg}</p>
              </div>
            </div>
            <Button variant="primary" size="lg" className="w-full" icon={ArrowRight} onClick={() => navigate('/password-dimenticata')}>
              Richiedi un nuovo link
            </Button>
          </div>
        )}

        <p className="text-sm text-anthracite-400 text-center mt-8">
          <Link to="/login" className="text-navy-700 font-medium hover:text-navy-900 transition-colors">Torna al login</Link>
        </p>
      </div>
    </div>
  );
}
