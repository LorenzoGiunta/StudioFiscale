import { useState, useEffect, useCallback } from 'react';
import { PageTitle } from '../../components/ui/Display.jsx';
import { RuoloBadge } from '../../components/ui/Badge.jsx';
import { Button, Input, Select } from '../../components/ui/FormControls.jsx';
import DataTable from '../../components/ui/DataTable.jsx';
import { useToast } from '../../contexts/ToastContext.jsx';
import { useAuth } from '../../contexts/AuthContext.jsx';
import adminService from '../../services/AdminService.js';
import { Filter, Search, UserCheck, UserX, Trash2, Loader2, RefreshCw, RotateCcw, ChevronDown, ChevronUp } from 'lucide-react';

/**
 * Pagina di gestione utenti dell'amministratore: elenco paginato e filtrabile,
 * con abilitazione, disabilitazione, cancellazione logica e ripristino.
 */
export default function AdminUtenti() {
  const toast = useToast();
  const { user } = useAuth();
  const [utenti, setUtenti] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState(null);
  const [filtroRuolo, setFiltroRuolo] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(1);           // 1-based per il DataTable
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [utentiEliminati, setUtentiEliminati] = useState([]);
  const [showEliminati, setShowEliminati] = useState(false);

  const loadData = useCallback(async (pageNum = page) => {
    setLoading(true);
    try {
      // Il backend usa pagine 0-based, il DataTable usa 1-based
      const [data, eliminati] = await Promise.allSettled([
        adminService.getUtenti(pageNum - 1, 20),
        adminService.getEliminati(),
      ]);
      if (data.status === 'fulfilled') {
        setUtenti(data.value.content || []);
        setTotalPages(data.value.totalPages || 1);
        setTotalElements(data.value.totalElements || 0);
      }
      if (eliminati.status === 'fulfilled') {
        setUtentiEliminati(eliminati.value || []);
      }
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel caricamento degli utenti');
    } finally {
      setLoading(false);
    }
  }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { loadData(page); }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  const handlePageChange = (newPage) => {
    setPage(newPage);
  };

  const handleAbilita = async (id, nome) => {
    setActionId(id);
    try {
      await adminService.abilita(id);
      toast.success(`${nome} abilitato`);
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nell\'abilitazione dell\'utente');
    } finally {
      setActionId(null);
    }
  };

  const handleDisabilita = async (id, nome) => {
    setActionId(id);
    try {
      await adminService.disabilita(id);
      toast.success(`${nome} disabilitato`);
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nella disabilitazione dell\'utente');
    } finally {
      setActionId(null);
    }
  };

  const handleRipristina = async (id, nome) => {
    setActionId(id);
    try {
      await adminService.ripristina(id);
      toast.success(`${nome} ripristinato`);
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nel ripristino dell\'utente');
    } finally {
      setActionId(null);
    }
  };

  const handleElimina = async (id, nome) => {
    if (!window.confirm(`Sei sicuro di voler eliminare ${nome}? L'azione è irreversibile.`)) return;
    setActionId(id);
    try {
      await adminService.elimina(id);
      toast.success(`${nome} eliminato`);
      await loadData(page);
    } catch (err) {
      toast.error(err.apiError?.message || 'Errore nell\'eliminazione dell\'utente');
    } finally {
      setActionId(null);
    }
  };

  // Filtro client-side sulla pagina corrente (ricerca e ruolo)
  const filtered = utenti.filter(u => {
    if (filtroRuolo && u.ruolo !== filtroRuolo) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return u.nome?.toLowerCase().includes(q) || u.cognome?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q);
    }
    return true;
  });

  const columns = [
    { key: 'nome', label: 'Utente', render: (_, row) => (
      <div>
        <p className="text-sm font-medium text-navy-900">{row.nome} {row.cognome}</p>
        <p className="text-xs text-anthracite-400">{row.email}</p>
      </div>
    )},
    { key: 'ruolo', label: 'Ruolo', render: (_, row) => <RuoloBadge ruolo={row.ruolo} />, width: '150px' },
    { key: 'abilitato', label: 'Stato', render: (_, row) => (
      <span className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold rounded-sm ${
        row.enabled ? 'text-emerald-600 bg-emerald-50' : 'text-red-600 bg-red-50'
      }`}>
        {row.enabled ? 'Attivo' : 'Disabilitato'}
      </span>
    ), width: '100px' },
    { key: 'azioni', label: 'Azioni', render: (_, row) => {
      // Un amministratore non può disabilitare o eliminare sé stesso né un altro
      // amministratore: le azioni distruttive restano nascoste su queste righe,
      // coerentemente con il vincolo applicato dal backend.
      const protetto = row.id === user?.id || row.ruolo === 'AMMINISTRATORE';
      return (
        <div className="flex items-center gap-1">
          {!row.enabled && (
            <Button variant="ghost" size="sm" icon={UserCheck}
              loading={actionId === row.id}
              onClick={() => handleAbilita(row.id, `${row.nome} ${row.cognome}`)}>
              Abilita
            </Button>
          )}
          {row.enabled && !protetto && (
            <Button variant="ghost" size="sm" icon={UserX}
              loading={actionId === row.id}
              onClick={() => handleDisabilita(row.id, `${row.nome} ${row.cognome}`)}>
              Disabilita
            </Button>
          )}
          {!protetto && (
            <button
              onClick={() => handleElimina(row.id, `${row.nome} ${row.cognome}`)}
              className="p-1.5 text-anthracite-300 hover:text-red-600 hover:bg-red-50 rounded-sm transition-colors ml-1"
              title="Elimina utente"
            >
              <Trash2 size={14} />
            </button>
          )}
          {protetto && row.enabled && (
            <span className="text-xs text-anthracite-300 italic px-1"
              title="Non è possibile disabilitare o eliminare un amministratore">
              Protetto
            </span>
          )}
        </div>
      );
    }, width: '180px' },
  ];

  return (
    <div className="animate-fade-in">
      <PageTitle
        subtitle={loading ? 'Caricamento...' : `${totalElements} utenti registrati nel sistema`}
        actions={<Button variant="ghost" icon={RefreshCw} onClick={() => loadData(page)} loading={loading} />}
      >
        Gestione Utenti
      </PageTitle>

      <div className="flex items-center gap-3 mb-4 flex-wrap">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-anthracite-300" />
          <input
            type="text" placeholder="Cerca utente..."
            value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9 pr-3 py-2 text-sm bg-white border border-anthracite-200 rounded-sm outline-none focus:border-navy-400 transition-colors w-64 placeholder:text-anthracite-300"
          />
        </div>
        <Select id="filter-ruolo" value={filtroRuolo} onChange={(e) => setFiltroRuolo(e.target.value)} className="w-48">
          <option value="">Tutti i ruoli</option>
          <option value="CLIENTE">Cliente</option>
          <option value="COMMERCIALISTA">Commercialista</option>
          <option value="COLLABORATORE">Collaboratore</option>
          <option value="AMMINISTRATORE">Amministratore</option>
        </Select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 size={24} className="animate-spin text-navy-400" />
          <span className="ml-3 text-sm text-anthracite-400">Caricamento utenti...</span>
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={filtered}
          page={page}
          totalPages={totalPages}
          onPageChange={handlePageChange}
        />
      )}

      {/* Sezione utenti eliminati (soft delete) */}
      {utentiEliminati.length > 0 && (
        <div className="mt-6">
          <button
            onClick={() => setShowEliminati(v => !v)}
            className="flex items-center gap-2 text-sm font-semibold text-anthracite-500 hover:text-navy-900 transition-colors mb-3"
          >
            {showEliminati ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
            Utenti eliminati
            <span className="px-2 py-0.5 text-xs font-bold bg-anthracite-200 text-anthracite-600 rounded-full">
              {utentiEliminati.length}
            </span>
          </button>

          {showEliminati && (
            <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="bg-anthracite-50/70">
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Utente</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Ruolo</th>
                    <th className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  {utentiEliminati.map(u => (
                    <tr key={u.id} className="border-b border-anthracite-50 last:border-b-0 opacity-75">
                      <td className="px-5 py-3.5">
                        <p className="text-sm font-medium text-navy-900">{u.nome} {u.cognome}</p>
                        <p className="text-xs text-anthracite-400">{u.email}</p>
                      </td>
                      <td className="px-5 py-3.5">
                        <RuoloBadge ruolo={u.ruolo} />
                      </td>
                      <td className="px-5 py-3.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          icon={RotateCcw}
                          loading={actionId === u.id}
                          onClick={() => handleRipristina(u.id, `${u.nome} ${u.cognome}`)}
                        >
                          Ripristina
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
