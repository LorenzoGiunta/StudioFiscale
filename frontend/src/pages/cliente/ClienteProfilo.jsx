import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { Button, Input, Select } from '../../components/ui/FormControls.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import clienteService from '../../services/ClienteService.js';
import { User, FileText, Euro, Save, Loader2, RefreshCw } from 'lucide-react';

const REGIMI = [
  { value: '', label: 'Non specificato' },
  { value: 'ORDINARIO', label: 'Regime Ordinario' },
  { value: 'FORFETTARIO', label: 'Regime Forfettario' },
];

/**
 * Pagina del profilo del cliente: visualizza e consente di aggiornare i dati
 * anagrafici e fiscali, tra cui il regime e il reddito usati per il calcolo delle imposte.
 */
export default function ClienteProfilo() {
  const toast = useToast();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    nome: '', cognome: '', email: '',
    codFiscale: '', pIVA: '', regime: '', redditoAnnuo: '',
  });
  const [errors, setErrors] = useState({});

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const data = await clienteService.getProfilo();
      setForm({
        nome: data.nome || '',
        cognome: data.cognome || '',
        email: data.email || '',
        codFiscale: data.codFiscale || '',
        pIVA: data.pIVA || '',
        regime: data.regime || '',
        redditoAnnuo: data.redditoAnnuo ?? '',
      });
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento del profilo');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const set = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: '' }));
  };

  const validate = () => {
    const e = {};
    if (!form.nome.trim()) e.nome = 'Obbligatorio';
    if (!form.cognome.trim()) e.cognome = 'Obbligatorio';
    if (!form.email.trim()) e.email = 'Obbligatoria';
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Email non valida';
    if (form.codFiscale && !/^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$/.test(form.codFiscale))
      e.codFiscale = 'Formato codice fiscale non valido';
    if (form.pIVA && !/^[0-9]{11}$/.test(form.pIVA))
      e.pIVA = 'Partita IVA: 11 cifre';
    if (form.redditoAnnuo !== '' && (isNaN(form.redditoAnnuo) || Number(form.redditoAnnuo) < 0))
      e.redditoAnnuo = 'Reddito non valido';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSaving(true);
    try {
      const payload = {
        nome: form.nome.trim(),
        cognome: form.cognome.trim(),
        email: form.email.trim(),
        codFiscale: form.codFiscale.trim() || null,
        pIVA: form.pIVA.trim() || null,
        regime: form.regime || null,
        redditoAnnuo: form.redditoAnnuo === '' ? null : Number(form.redditoAnnuo),
      };
      await clienteService.aggiornaProfilo(payload);
      toast.success('Profilo aggiornato con successo');
      await loadData();
    } catch (err) {
      toast.error(err.apiError?.message || err.message || "Errore nell'aggiornamento del profilo");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento profilo...</span>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle="Gestisci i tuoi dati anagrafici e fiscali"
        actions={<Button variant="ghost" icon={RefreshCw} onClick={loadData} />}
      >
        Il mio profilo
      </PageTitle>

      <form onSubmit={handleSave} className="space-y-6 max-w-3xl">
        {/* Anagrafica */}
        <div className="bg-white border border-anthracite-100 rounded-sm p-6">
          <div className="flex items-center gap-2 mb-5">
            <User size={16} className="text-navy-500" />
            <h3 className="text-base font-semibold text-navy-900">Dati anagrafici</h3>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input id="prof-nome" label="Nome *" value={form.nome} onChange={set('nome')} error={errors.nome} />
            <Input id="prof-cognome" label="Cognome *" value={form.cognome} onChange={set('cognome')} error={errors.cognome} />
            <Input id="prof-email" type="email" label="Email *" value={form.email} onChange={set('email')} error={errors.email} className="sm:col-span-2" />
          </div>
        </div>

        {/* Dati fiscali */}
        <div className="bg-white border border-anthracite-100 rounded-sm p-6">
          <div className="flex items-center gap-2 mb-5">
            <FileText size={16} className="text-navy-500" />
            <h3 className="text-base font-semibold text-navy-900">Dati fiscali</h3>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              id="prof-cf"
              label="Codice fiscale"
              value={form.codFiscale}
              onChange={(e) => { const v = e.target.value.toUpperCase(); setForm(p => ({ ...p, codFiscale: v })); if (errors.codFiscale) setErrors(p => ({ ...p, codFiscale: '' })); }}
              error={errors.codFiscale}
              placeholder="RSSMRA85M01H501Z"
              maxLength={16}
            />
            <Input
              id="prof-piva"
              label="Partita IVA"
              value={form.pIVA}
              onChange={set('pIVA')}
              error={errors.pIVA}
              placeholder="12345678901"
              maxLength={11}
            />
            <div>
              <label htmlFor="prof-regime" className="block text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider mb-1.5">
                Regime fiscale
              </label>
              <Select id="prof-regime" value={form.regime} onChange={set('regime')}>
                {REGIMI.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
              </Select>
            </div>
            <Input
              id="prof-reddito"
              type="number"
              label="Reddito annuo (€)"
              value={form.redditoAnnuo}
              onChange={set('redditoAnnuo')}
              error={errors.redditoAnnuo}
              placeholder="0"
              min="0"
              step="0.01"
            />
          </div>
        </div>

        {/* Nota informativa */}
        <div className="bg-navy-50 border border-navy-100 rounded-sm p-4 flex items-start gap-3">
          <Euro size={18} className="text-navy-500 mt-0.5 shrink-0" />
          <p className="text-xs text-navy-700 leading-relaxed">
            I dati fiscali (regime e reddito annuo) sono usati dal tuo commercialista per il calcolo
            delle imposte. Tienili aggiornati per ricevere stime precise.
          </p>
        </div>

        {/* Actions */}
        <div className="flex justify-end">
          <Button type="submit" variant="primary" size="lg" icon={Save} loading={saving}>
            Salva modifiche
          </Button>
        </div>
      </form>
    </div>
  );
}
