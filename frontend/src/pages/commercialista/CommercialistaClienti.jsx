import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { PageTitle } from '../../components/ui/Display.jsx';
import { Button, Select } from '../../components/ui/FormControls.jsx';
import DataTable from '../../components/ui/DataTable.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import commercialistaService from '../../services/CommercialistaService.js';
import { Search, RefreshCw, Loader2 } from 'lucide-react';

/**
 * Pagina dei clienti del commercialista: elenco ricercabile dei propri clienti
 * con accesso alla scheda di dettaglio di ciascuno.
 */
export default function CommercialistaClienti() {
  const toast = useToast();
  const navigate = useNavigate();
  const [clienti, setClienti] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filtroRegime, setFiltroRegime] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setClienti(await commercialistaService.getClienti());
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento dei clienti');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = clienti.filter(c => {
    if (filtroRegime && c.regime !== filtroRegime) return false;
    if (search) {
      const q = search.toLowerCase();
      return c.nome?.toLowerCase().includes(q)
        || c.cognome?.toLowerCase().includes(q)
        || c.email?.toLowerCase().includes(q)
        || c.codFiscale?.toLowerCase().includes(q);
    }
    return true;
  });

  const columns = [
    { key: 'nome', label: 'Cliente', render: (_, row) => (
      <div>
        <p className="text-sm font-medium text-navy-900">{row.nomeCompleto}</p>
        <p className="text-xs text-anthracite-400">{row.email}</p>
      </div>
    )},
    { key: 'codFiscale', label: 'Codice fiscale', render: (v) => v || '—' },
    { key: 'regime', label: 'Regime', render: (_, row) => row.regimeLabel },
    { key: 'redditoAnnuo', label: 'Reddito annuo', render: (v) =>
      v != null ? new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(v) : '—' },
    { key: 'enabled', label: 'Stato', render: (_, row) => (
      <span className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold rounded-sm ${
        row.enabled ? 'text-emerald-600 bg-emerald-50' : 'text-red-600 bg-red-50'
      }`}>
        {row.enabled ? 'Attivo' : 'Disabilitato'}
      </span>
    ), width: '100px' },
  ];

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento clienti...</span>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle={`${clienti.length} clienti registrati`}
        actions={<Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />}
      >
        Clienti
      </PageTitle>

      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-anthracite-300" />
          <input
            type="text" placeholder="Cerca cliente..."
            value={search} onChange={(e) => setSearch(e.target.value)}
            className="pl-9 pr-3 py-2 text-sm bg-white border border-anthracite-200 rounded-sm outline-none focus:border-navy-400 transition-colors w-64 placeholder:text-anthracite-300"
          />
        </div>
        <Select id="filter-regime" value={filtroRegime} onChange={(e) => setFiltroRegime(e.target.value)} className="w-48">
          <option value="">Tutti i regimi</option>
          <option value="ORDINARIO">Ordinario</option>
          <option value="FORFETTARIO">Forfettario</option>
        </Select>
      </div>

      <DataTable columns={columns} data={filtered} onRowClick={(c) => navigate(`${c.id}`)} />
    </div>
  );
}
