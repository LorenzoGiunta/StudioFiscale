/**
 * Raccolta di controlli per i form (pulsanti, campi di input e simili) con stile
 * e comportamento uniformi, riutilizzati nelle varie pagine dell'applicazione.
 */
export function Button({ children, variant = 'primary', size = 'md', icon: Icon, disabled = false, loading = false, onClick, type = 'button', className = '' }) {
  const variants = {
    primary: 'bg-navy-900 text-white hover:bg-navy-800 active:bg-navy-950',
    secondary: 'bg-white text-navy-900 border border-anthracite-200 hover:bg-anthracite-50 active:bg-anthracite-100',
    accent: 'bg-amber-500 text-white hover:bg-amber-600 active:bg-amber-700',
    ghost: 'text-anthracite-500 hover:bg-anthracite-50 hover:text-navy-900',
    danger: 'bg-red-600 text-white hover:bg-red-700 active:bg-red-800',
  };
  const sizes = {
    sm: 'px-3 py-1.5 text-xs',
    md: 'px-4 py-2 text-sm',
    lg: 'px-5 py-2.5 text-sm',
  };
  return (
    <button type={type} onClick={onClick} disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 font-medium rounded-sm transition-all duration-150 disabled:opacity-50 disabled:cursor-not-allowed ${variants[variant]} ${sizes[size]} ${className}`}>
      {loading ? (
        <div className="w-4 h-4 border-2 border-current/30 border-t-current rounded-full animate-spin" />
      ) : Icon ? (
        <Icon size={size === 'sm' ? 14 : 16} strokeWidth={1.75} />
      ) : null}
      {children}
    </button>
  );
}

export function Input({ label, error, type = 'text', id, className = '', ...props }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="block text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider mb-1.5">{label}</label>
      )}
      <input id={id} type={type}
        className={`w-full px-3 py-2.5 text-sm bg-white border rounded-sm transition-all duration-150 outline-none ${
          error ? 'border-red-400 focus:border-red-500 focus:ring-1 focus:ring-red-500/20' : 'border-anthracite-200 focus:border-navy-400 focus:ring-1 focus:ring-navy-400/20'
        } placeholder:text-anthracite-300`}
        {...props}
      />
      {error && <p className="text-xs text-red-500 mt-1 font-medium">{error}</p>}
    </div>
  );
}

export function Select({ label, error, id, children, className = '', ...props }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="block text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider mb-1.5">{label}</label>
      )}
      <select id={id}
        className={`w-full px-3 py-2.5 text-sm bg-white border rounded-sm transition-all duration-150 outline-none ${
          error ? 'border-red-400 focus:border-red-500 focus:ring-1 focus:ring-red-500/20' : 'border-anthracite-200 focus:border-navy-400 focus:ring-1 focus:ring-navy-400/20'
        }`}
        {...props}>
        {children}
      </select>
      {error && <p className="text-xs text-red-500 mt-1 font-medium">{error}</p>}
    </div>
  );
}

export function Textarea({ label, error, id, className = '', ...props }) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="block text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider mb-1.5">{label}</label>
      )}
      <textarea id={id}
        className={`w-full px-3 py-2.5 text-sm bg-white border rounded-sm transition-all duration-150 outline-none resize-y min-h-[80px] ${
          error ? 'border-red-400 focus:border-red-500 focus:ring-1 focus:ring-red-500/20' : 'border-anthracite-200 focus:border-navy-400 focus:ring-1 focus:ring-navy-400/20'
        } placeholder:text-anthracite-300`}
        {...props}
      />
      {error && <p className="text-xs text-red-500 mt-1 font-medium">{error}</p>}
    </div>
  );
}
