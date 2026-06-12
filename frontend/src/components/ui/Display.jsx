/**
 * Raccolta di componenti UI di presentazione (schede statistiche, titoli di
 * pagina ed elementi affini) usati per uniformare l'aspetto delle viste.
 */
export function StatsCard({ icon: Icon, label, value, trend, trendLabel, accent = false }) {
  return (
    <div className={`border rounded-sm p-5 transition-all duration-200 hover:shadow-sm ${accent ? 'bg-navy-900 border-navy-800 text-white' : 'bg-white border-anthracite-100'}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className={`text-[11px] font-semibold uppercase tracking-wider mb-2 ${accent ? 'text-navy-300' : 'text-anthracite-400'}`}>{label}</p>
          <p className={`text-2xl font-bold tracking-tight ${accent ? 'text-white' : 'text-navy-900'}`}>{value}</p>
          {trend !== undefined && (
            <p className={`text-xs mt-1.5 font-medium ${trend > 0 ? 'text-emerald-500' : trend < 0 ? 'text-red-400' : accent ? 'text-navy-300' : 'text-anthracite-400'}`}>
              {trend > 0 ? '+' : ''}{trend}% {trendLabel}
            </p>
          )}
        </div>
        {Icon && (
          <div className={`w-10 h-10 rounded-sm flex items-center justify-center ${accent ? 'bg-amber-500/20 text-amber-400' : 'bg-navy-50 text-navy-400'}`}>
            <Icon size={20} strokeWidth={1.75} />
          </div>
        )}
      </div>
    </div>
  );
}

export function SectionTitle({ children, action }) {
  return (
    <div className="flex items-center justify-between mb-4">
      <h3 className="text-base font-semibold text-navy-900">{children}</h3>
      {action}
    </div>
  );
}

export function PageTitle({ children, subtitle, actions }) {
  return (
    <div className="flex items-start justify-between mb-6">
      <div>
        <h1 className="text-xl font-bold text-navy-900 tracking-tight">{children}</h1>
        {subtitle && <p className="text-sm text-anthracite-400 mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}

export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6">
      {Icon && (
        <div className="w-14 h-14 rounded-sm bg-anthracite-50 flex items-center justify-center mb-4">
          <Icon size={24} strokeWidth={1.5} className="text-anthracite-300" />
        </div>
      )}
      <h3 className="text-sm font-semibold text-navy-900 mb-1">{title}</h3>
      <p className="text-sm text-anthracite-400 text-center max-w-sm mb-4">{description}</p>
      {action}
    </div>
  );
}

export function LoadingSpinner({ className = '' }) {
  return (
    <div className={`flex items-center justify-center py-12 ${className}`}>
      <div className="relative w-8 h-8">
        <div className="absolute inset-0 border-2 border-anthracite-200 rounded-full" />
        <div className="absolute inset-0 border-2 border-transparent border-t-navy-600 rounded-full animate-spin" />
      </div>
    </div>
  );
}
