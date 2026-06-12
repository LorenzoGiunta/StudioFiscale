import { ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * Tabella dati riutilizzabile.
 *
 * Rende un insieme di righe secondo una configurazione di colonne e gestisce la
 * navigazione tra le pagine, così da uniformare la presentazione degli elenchi.
 */
export default function DataTable({
  columns,
  data,
  loading = false,
  emptyMessage = 'Nessun dato disponibile',
  page = 1,
  totalPages = 1,
  onPageChange,
  onRowClick,
}) {
  if (loading) {
    return (
      <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
        <div className="animate-pulse-soft">
          <div className="h-11 bg-anthracite-50 border-b border-anthracite-100" />
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className="h-14 border-b border-anthracite-50 flex items-center px-5 gap-4"
            >
              <div className="h-3 bg-anthracite-100 rounded w-1/4" />
              <div className="h-3 bg-anthracite-100 rounded w-1/3" />
              <div className="h-3 bg-anthracite-100 rounded w-1/6" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white border border-anthracite-100 rounded-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="bg-anthracite-50/70">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className="text-left text-[11px] font-semibold text-anthracite-500 uppercase tracking-wider px-5 py-3 border-b border-anthracite-100"
                  style={col.width ? { width: col.width } : undefined}
                >
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-5 py-12 text-center text-sm text-anthracite-400"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((row, i) => (
                <tr
                  key={row.id || i}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  className={`border-b border-anthracite-50 last:border-b-0 hover:bg-anthracite-50/40 transition-colors ${onRowClick ? 'cursor-pointer' : ''}`}
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className="px-5 py-3.5 text-sm text-navy-800"
                    >
                      {col.render ? col.render(row[col.key], row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between px-5 py-3 border-t border-anthracite-100 bg-anthracite-50/30">
          <span className="text-xs text-anthracite-400">
            Pagina {page} di {totalPages}
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => onPageChange?.(page - 1)}
              disabled={page <= 1}
              className="p-1.5 text-anthracite-500 hover:text-navy-900 hover:bg-anthracite-100 rounded-sm transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronLeft size={16} />
            </button>
            {[...Array(Math.min(totalPages, 7))].map((_, i) => {
              const pageNum = i + 1;
              return (
                <button
                  key={pageNum}
                  onClick={() => onPageChange?.(pageNum)}
                  className={`w-8 h-8 text-xs font-medium rounded-sm transition-colors ${
                    pageNum === page
                      ? 'bg-navy-900 text-white'
                      : 'text-anthracite-500 hover:bg-anthracite-100'
                  }`}
                >
                  {pageNum}
                </button>
              );
            })}
            <button
              onClick={() => onPageChange?.(page + 1)}
              disabled={page >= totalPages}
              className="p-1.5 text-anthracite-500 hover:text-navy-900 hover:bg-anthracite-100 rounded-sm transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
