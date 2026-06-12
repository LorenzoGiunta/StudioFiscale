/**
 * Componenti UI per la visualizzazione degli stati come etichette colorate
 * (badge), con stile e testo coerenti per ciascuno stato di dominio.
 */
const STATO_CONFIG = {
  BOZZA: {
    label: 'Bozza',
    bg: 'bg-anthracite-100',
    text: 'text-anthracite-600',
    dot: 'bg-anthracite-400',
  },
  IN_LAVORAZIONE: {
    label: 'In Lavorazione',
    bg: 'bg-blue-50',
    text: 'text-blue-700',
    dot: 'bg-blue-500',
  },
  IN_ATTESA_DOCUMENTI: {
    label: 'In Attesa Documenti',
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    dot: 'bg-amber-500',
  },
  COMPLETATA: {
    label: 'Completata',
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    dot: 'bg-emerald-500',
  },
};

// Etichetta leggibile dello stato pratica, per i contesti senza badge (es. <option>)
export function statoPraticaLabel(stato) {
  return STATO_CONFIG[stato]?.label || stato;
}

export function StatoBadge({ stato }) {
  const config = STATO_CONFIG[stato] || STATO_CONFIG.BOZZA;
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wider rounded-sm ${config.bg} ${config.text}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
}

export function RuoloBadge({ ruolo }) {
  const config = {
    CLIENTE: { bg: 'bg-navy-50', text: 'text-navy-700' },
    COMMERCIALISTA: { bg: 'bg-amber-50', text: 'text-amber-800' },
    COLLABORATORE: { bg: 'bg-blue-50', text: 'text-blue-700' },
    AMMINISTRATORE: { bg: 'bg-red-50', text: 'text-red-700' },
  };
  const c = config[ruolo] || config.CLIENTE;
  const labels = {
    CLIENTE: 'Cliente',
    COMMERCIALISTA: 'Commercialista',
    COLLABORATORE: 'Collaboratore',
    AMMINISTRATORE: 'Admin',
  };
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wider rounded-sm ${c.bg} ${c.text}`}
    >
      {labels[ruolo] || ruolo}
    </span>
  );
}
