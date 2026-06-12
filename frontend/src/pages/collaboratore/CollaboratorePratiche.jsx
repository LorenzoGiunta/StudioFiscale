import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { StatoBadge } from '../../components/ui/Badge.jsx';
import DataTable from '../../components/ui/DataTable.jsx';
import { Select, Button } from '../../components/ui/FormControls.jsx';
import { Filter, Loader2, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../../contexts/ToastContext.jsx';
import collaboratoreService from '../../services/CollaboratoreService.js';

/**
 * Pagina delle pratiche assegnate al collaboratore, con stato, filtri e accesso
 * al dettaglio di ciascuna pratica.
 */
export default function CollaboratorePratiche() {
  const toast = useToast();
  const navigate = useNavigate();
  const [pratiche, setPratiche] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroStato, setFiltroStato] = useState('');
  const [page, setPage] = useState(1);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const data = await collaboratoreService.getPratiche();
      setPratiche(data);
    } catch {
      toast.error('Errore nel caricamento delle pratiche assegnate');
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(); }, [loadData]);

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  const filtered = filtroStato ? pratiche.filter(p => p.stato === filtroStato) : pratiche;

  const columns = [
    {
      key: 'tipoPratica',
      label: 'Tipo pratica',
      render: (v) => (
        <span className="font-medium text-navy-900">{v?.replace(/_/g, ' ') || '—'}</span>
      ),
    },
    {
      key: 'nomeCliente',
      label: 'Cliente',
      render: (v) => <span className="text-anthracite-600">{v || '—'}</span>,
    },
    {
      key: 'stato',
      label: 'Stato',
      render: (_, row) => <StatoBadge stato={row.stato} />,
      width: '180px',
    },
    {
      key: 'scadenza',
      label: 'Scadenza',
      render: (v) => formatDate(v),
    },
    {
      key: 'dataCreazione',
      label: 'Creata il',
      render: (v) => formatDate(v),
    },
  ];

  if (loading) {
    return (
      <div className="animate-fade-in flex items-center justify-center py-20">
        <Loader2 size={24} className="animate-spin text-navy-400" />
        <span className="ml-3 text-sm text-anthracite-400">Caricamento pratiche...</span>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle={`${pratiche.length} pratiche assegnate`}
        actions={<Button variant="ghost" icon={RefreshCw} onClick={loadData} loading={loading} />}
      >
        Pratiche Assegnate
      </PageTitle>

      <div className="flex items-center gap-3 mb-4">
        <Filter size={15} className="text-anthracite-400" />
        <Select
          id="filter-stato"
          value={filtroStato}
          onChange={(e) => setFiltroStato(e.target.value)}
          className="w-48"
        >
          <option value="">Tutti gli stati</option>
          <option value="BOZZA">Bozza</option>
          <option value="IN_LAVORAZIONE">In Lavorazione</option>
          <option value="IN_ATTESA_DOCUMENTI">In Attesa Documenti</option>
          <option value="COMPLETATA">Completata</option>
        </Select>
      </div>

      <DataTable
        columns={columns}
        data={filtered}
        page={page}
        totalPages={1}
        onPageChange={setPage}
        onRowClick={(p) => navigate(`${p.id}`)}
      />
    </div>
  );
}
