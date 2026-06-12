import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import authService from '../../services/AuthService.js';
import { Input, Select, Button } from '../../components/ui/FormControls.jsx';
import { ArrowRight, ArrowLeft, Loader2 } from 'lucide-react';

const RUOLI = [
  { value: '', label: 'Seleziona ruolo' },
  { value: 'CLIENTE', label: 'Cliente' },
  { value: 'COMMERCIALISTA', label: 'Commercialista' },
  { value: 'COLLABORATORE', label: 'Collaboratore' },
];

const REGIMI = [
  { value: '', label: 'Seleziona regime' },
  { value: 'ORDINARIO', label: 'Regime Ordinario' },
  { value: 'FORFETTARIO', label: 'Regime Forfettario' },
];

/**
 * Pagina di registrazione: raccoglie i dati del nuovo utente, mostra i campi
 * pertinenti al ruolo selezionato e, al termine, autentica e instrada l'utente.
 */
export default function RegisterPage() {
  const navigate = useNavigate();
  const { loginUser } = useAuth();
  const toast = useToast();
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({
    ruolo: '', nome: '', cognome: '', email: '', password: '', confirmPassword: '',
    codiceFiscale: '', partitaIva: '', regimeFiscale: '', redditoAnnuo: '',
    numeroAlbo: '',
  });
  const [commercialistaId, setCommercialistaId] = useState('');
  const [commercialisti, setCommercialisti] = useState([]);
  const [loadingComm, setLoadingComm] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const set = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
  };

  const validateStep1 = () => {
    const e = {};
    if (!form.ruolo) e.ruolo = 'Seleziona un ruolo';
    if (!form.nome.trim()) e.nome = 'Nome obbligatorio';
    if (!form.cognome.trim()) e.cognome = 'Cognome obbligatorio';
    if (!form.email) e.email = 'Email obbligatoria';
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Email non valida';
    if (!form.password) e.password = 'Password obbligatoria';
    else if (!/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\-]).{8,}$/.test(form.password))
      e.password = 'Min. 8 caratteri con maiuscola, minuscola, numero e carattere speciale (@#$%^&+=!_-)';
    if (form.password !== form.confirmPassword) e.confirmPassword = 'Le password non corrispondono';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const validateStep2 = () => {
    const e = {};
    if (form.ruolo === 'CLIENTE') {
      if (!form.codiceFiscale.trim()) e.codiceFiscale = 'Codice fiscale obbligatorio';
      else if (!/^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$/.test(form.codiceFiscale.toUpperCase()))
        e.codiceFiscale = 'Formato codice fiscale non valido (es. RSSMRA85T10A562S)';
      if (form.partitaIva && !/^[0-9]{11}$/.test(form.partitaIva))
        e.partitaIva = 'La partita IVA deve avere esattamente 11 cifre';
      if (!form.regimeFiscale) e.regimeFiscale = 'Seleziona un regime';
      if (!commercialistaId) e.commercialistaId = 'Seleziona il tuo commercialista';
    }
    if (form.ruolo === 'COMMERCIALISTA') {
      if (!form.numeroAlbo.trim()) e.numeroAlbo = 'Numero iscrizione obbligatorio';
      else if (!/^\d+([\/\-][A-Za-z0-9]+)?$/.test(form.numeroAlbo.trim()))
        e.numeroAlbo = 'Formato non valido (es. 12345 oppure 12345/A)';
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };


  const handleNext = () => {
    if (validateStep1()) {
      if (form.ruolo === 'COLLABORATORE') {
        handleSubmit();
      } else {
        // Carica commercialisti solo quando il cliente passa allo step 2
        if (form.ruolo === 'CLIENTE' && commercialisti.length === 0) {
          setLoadingComm(true);
          authService.getCommercialisti()
            .then(data => setCommercialisti(data || []))
            .catch(() => {})
            .finally(() => setLoadingComm(false));
        }
        setStep(2);
      }
    }
  };

  const handleSubmit = async () => {
    if (step === 2 && !validateStep2()) return;
    setLoading(true);
    try {
      // Build payload matching backend RegistrazioneRequest DTO
      const payload = {
        nome: form.nome,
        cognome: form.cognome,
        email: form.email,
        password: form.password,
        ruolo: form.ruolo,
      };

      // Campi specifici Cliente
      if (form.ruolo === 'CLIENTE') {
        payload.codFiscale = form.codiceFiscale.toUpperCase();
        payload.pIVA = form.partitaIva || null;
        payload.regime = form.regimeFiscale;
        payload.redditoAnnuo = form.redditoAnnuo ? parseFloat(form.redditoAnnuo) : null;
        payload.commercialistaId = parseInt(commercialistaId);
      }

      // Campo specifico Commercialista
      if (form.ruolo === 'COMMERCIALISTA') {
        payload.numeroAlbo = form.numeroAlbo;
      }

      const data = await authService.register(payload);
      // AuthResponse: { token, id, ruolo, email, nome, cognome }
      const userData = {
        id: data.id,
        nome: data.nome,
        cognome: data.cognome,
        email: data.email,
        ruolo: data.ruolo,
      };
      loginUser(userData, data.token);
      toast.success('Registrazione completata con successo');

      const routes = {
        CLIENTE: '/dashboard/cliente',
        COMMERCIALISTA: '/dashboard/commercialista',
        COLLABORATORE: '/dashboard/collaboratore',
      };
      setTimeout(() => navigate(routes[data.ruolo] || '/login'), 100);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Errore durante la registrazione';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const needsStep2 = form.ruolo === 'CLIENTE' || form.ruolo === 'COMMERCIALISTA';

  return (
    <div className="min-h-screen flex">
      {/* Left branding */}
      <div className="hidden lg:flex lg:w-[45%] bg-navy-900 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-navy-900 via-navy-800 to-navy-950" />
        <div className="absolute top-0 right-0 w-96 h-96 bg-amber-500/5 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-navy-400/10 rounded-full blur-3xl" />
        <div className="relative z-10 flex flex-col justify-between p-12 w-full">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
              <span className="text-navy-900 font-extrabold text-base">SF</span>
            </div>
            <div>
              <h1 className="text-lg font-bold text-white tracking-tight leading-none">StudioFiscale</h1>
              <span className="text-[10px] text-navy-400 tracking-widest uppercase font-medium">Gestionale</span>
            </div>
          </div>
          <div>
            <h2 className="text-3xl font-bold text-white leading-tight tracking-tight mb-4">
              Unisciti a<br /><span className="text-amber-400">StudioFiscale.</span>
            </h2>
            <p className="text-sm text-navy-300 leading-relaxed max-w-sm">
              Crea il tuo account e inizia a gestire le tue pratiche fiscali in modo semplice e professionale.
            </p>
          </div>
        </div>
      </div>

      {/* Right form */}
      <div className="flex-1 flex items-center justify-center p-6 bg-white overflow-y-auto">
        <div className="w-full max-w-md">
          <div className="lg:hidden flex items-center gap-3 mb-10">
            <div className="w-9 h-9 bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center rounded-sm">
              <span className="text-navy-900 font-extrabold text-sm">SF</span>
            </div>
            <h1 className="text-lg font-bold text-navy-900 tracking-tight">StudioFiscale</h1>
          </div>

          {/* Progress */}
          {needsStep2 && (
            <div className="flex items-center gap-3 mb-6">
              <div className={`flex items-center gap-2 ${step >= 1 ? 'text-navy-900' : 'text-anthracite-300'}`}>
                <div className={`w-6 h-6 rounded-full text-xs font-semibold flex items-center justify-center ${step >= 1 ? 'bg-navy-900 text-white' : 'bg-anthracite-100 text-anthracite-400'}`}>1</div>
                <span className="text-xs font-medium">Dati account</span>
              </div>
              <div className="flex-1 h-px bg-anthracite-200" />
              <div className={`flex items-center gap-2 ${step >= 2 ? 'text-navy-900' : 'text-anthracite-300'}`}>
                <div className={`w-6 h-6 rounded-full text-xs font-semibold flex items-center justify-center ${step >= 2 ? 'bg-navy-900 text-white' : 'bg-anthracite-100 text-anthracite-400'}`}>2</div>
                <span className="text-xs font-medium">Dati profilo</span>
              </div>
            </div>
          )}

          <div className="mb-6">
            <h2 className="text-xl font-bold text-navy-900 tracking-tight mb-1">
              {step === 1 ? 'Crea il tuo account' : 'Completa il profilo'}
            </h2>
            <p className="text-sm text-anthracite-400">
              {step === 1 ? 'Inserisci i tuoi dati per registrarti' : `Dati aggiuntivi per il ruolo ${form.ruolo.toLowerCase()}`}
            </p>
          </div>

          {step === 1 && (
            <div className="space-y-4">
              <Select id="reg-ruolo" label="Ruolo" value={form.ruolo} onChange={set('ruolo')} error={errors.ruolo}>
                {RUOLI.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
              </Select>
              <div className="grid grid-cols-2 gap-3">
                <Input id="reg-nome" label="Nome" placeholder="Mario" value={form.nome} onChange={set('nome')} error={errors.nome} />
                <Input id="reg-cognome" label="Cognome" placeholder="Rossi" value={form.cognome} onChange={set('cognome')} error={errors.cognome} />
              </div>
              <Input id="reg-email" label="Email" type="email" placeholder="nome@studio.it" value={form.email} onChange={set('email')} error={errors.email} />
              <Input id="reg-password" label="Password" type="password" placeholder="Minimo 8 caratteri" value={form.password} onChange={set('password')} error={errors.password} />
              <Input id="reg-confirm" label="Conferma Password" type="password" placeholder="Ripeti la password" value={form.confirmPassword} onChange={set('confirmPassword')} error={errors.confirmPassword} />
              <Button type="button" variant="primary" size="lg" className="w-full mt-2" icon={ArrowRight} onClick={handleNext} loading={loading && !needsStep2}>
                {needsStep2 ? 'Continua' : 'Registrati'}
              </Button>
            </div>
          )}

          {step === 2 && form.ruolo === 'CLIENTE' && (
            <div className="space-y-4">
              <Input id="reg-cf" label="Codice Fiscale" placeholder="RSSMRA85T10A562S" value={form.codiceFiscale} onChange={set('codiceFiscale')} error={errors.codiceFiscale} maxLength={16} className="uppercase" />
              <Input id="reg-piva" label="Partita IVA (opzionale)" placeholder="12345678901" value={form.partitaIva} onChange={set('partitaIva')} error={errors.partitaIva} maxLength={11} />
              <Select id="reg-regime" label="Regime Fiscale" value={form.regimeFiscale} onChange={set('regimeFiscale')} error={errors.regimeFiscale}>
                {REGIMI.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
              </Select>
              <Input id="reg-reddito" label="Reddito Annuo (€)" type="number" placeholder="35000" value={form.redditoAnnuo} onChange={set('redditoAnnuo')} />

              {/* Selezione commercialista */}
              <div>
                <label className="block text-xs font-medium text-anthracite-500 mb-1.5">
                  Commercialista *
                </label>
                {loadingComm ? (
                  <div className="flex items-center gap-2 text-sm text-anthracite-400 py-2">
                    <Loader2 size={14} className="animate-spin" />
                    Caricamento commercialisti...
                  </div>
                ) : (
                  <Select
                    id="reg-commercialista"
                    value={commercialistaId}
                    onChange={(e) => { setCommercialistaId(e.target.value); if (errors.commercialistaId) setErrors(p => ({ ...p, commercialistaId: '' })); }}
                    error={errors.commercialistaId}
                  >
                    <option value="">Seleziona il tuo commercialista...</option>
                    {commercialisti.map(c => (
                      <option key={c.id} value={c.id}>
                        {c.nome} {c.cognome}
                      </option>
                    ))}
                  </Select>
                )}
                {errors.commercialistaId && (
                  <p className="text-xs text-red-500 mt-1">{errors.commercialistaId}</p>
                )}
              </div>

              <div className="flex gap-3 mt-2">
                <Button variant="secondary" size="lg" icon={ArrowLeft} onClick={() => setStep(1)} className="flex-1">Indietro</Button>
                <Button variant="primary" size="lg" icon={ArrowRight} onClick={handleSubmit} loading={loading} className="flex-1">Registrati</Button>
              </div>
            </div>
          )}

          {step === 2 && form.ruolo === 'COMMERCIALISTA' && (
            <div className="space-y-4">
              <Input id="reg-albo" label="Numero Iscrizione Albo" placeholder="Es. 12345/A" value={form.numeroAlbo} onChange={set('numeroAlbo')} error={errors.numeroAlbo} />
              <p className="text-xs text-anthracite-400">Il numero di albo verrà verificato dall'amministratore prima dell'attivazione completa dell'account.</p>
              <div className="flex gap-3 mt-2">
                <Button variant="secondary" size="lg" icon={ArrowLeft} onClick={() => setStep(1)} className="flex-1">Indietro</Button>
                <Button variant="primary" size="lg" icon={ArrowRight} onClick={handleSubmit} loading={loading} className="flex-1">Registrati</Button>
              </div>
            </div>
          )}

          <p className="text-sm text-anthracite-400 text-center mt-6">
            Hai già un account?{' '}
            <Link to="/login" className="text-navy-700 font-medium hover:text-navy-900 transition-colors">Accedi</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
