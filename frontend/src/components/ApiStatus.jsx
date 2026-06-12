import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../api/client.js';
import { Activity, CheckCircle, XCircle, RefreshCw, Server, Wifi } from 'lucide-react';

/**
 * Componente diagnostico per verificare la connessione al backend.
 *
 * Esegue una richiesta verso un endpoint configurabile e ne mostra l'esito in
 * tempo reale, utile in fase di sviluppo per controllare la raggiungibilità delle API.
 */
export default function ApiStatus() {
  const [endpoint, setEndpoint] = useState('/api/auth/status');

  const { data, error, isLoading, isError, isSuccess, refetch, dataUpdatedAt } = useQuery({
    queryKey: ['api-status', endpoint],
    queryFn: async () => {
      const res = await apiClient.get(endpoint);
      return res.data;
    },
    retry: false,
    refetchOnWindowFocus: false,
  });

  const statusColor = isLoading
    ? 'var(--color-amber-400)'
    : isSuccess
      ? 'var(--color-emerald-400)'
      : 'var(--color-red-400)';

  return (
    <div style={{
      maxWidth: 520,
      margin: '2rem auto',
      fontFamily: "'Inter', system-ui, sans-serif",
    }}>
      {/* Card */}
      <div style={{
        background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
        borderRadius: 16,
        border: '1px solid rgba(255,255,255,0.08)',
        overflow: 'hidden',
        boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
      }}>
        {/* Header */}
        <div style={{
          padding: '1.5rem 1.75rem',
          borderBottom: '1px solid rgba(255,255,255,0.06)',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
        }}>
          <div style={{
            width: 40, height: 40,
            borderRadius: 10,
            background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Server size={20} color="#fff" />
          </div>
          <div>
            <h3 style={{ color: '#f1f5f9', fontSize: 16, fontWeight: 600, margin: 0 }}>
              Stato Connessione API
            </h3>
            <p style={{ color: '#94a3b8', fontSize: 13, margin: 0 }}>
              Backend · localhost:8080
            </p>
          </div>
          {/* Status dot */}
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{
              width: 10, height: 10, borderRadius: '50%',
              background: statusColor,
              boxShadow: `0 0 12px ${statusColor}`,
              display: 'inline-block',
              animation: isLoading ? 'pulse 1.5s ease-in-out infinite' : 'none',
            }} />
            <span style={{ color: '#cbd5e1', fontSize: 13, fontWeight: 500 }}>
              {isLoading ? 'Connessione...' : isSuccess ? 'Online' : 'Offline'}
            </span>
          </div>
        </div>

        {/* Body */}
        <div style={{ padding: '1.5rem 1.75rem' }}>
          {/* Endpoint input */}
          <label style={{ color: '#94a3b8', fontSize: 12, fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Endpoint di test
          </label>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <input
              id="api-status-endpoint"
              type="text"
              value={endpoint}
              onChange={(e) => setEndpoint(e.target.value)}
              style={{
                flex: 1,
                padding: '10px 14px',
                borderRadius: 10,
                border: '1px solid rgba(255,255,255,0.1)',
                background: 'rgba(255,255,255,0.04)',
                color: '#e2e8f0',
                fontSize: 14,
                fontFamily: "'JetBrains Mono', monospace",
                outline: 'none',
              }}
              placeholder="/api/auth/status"
            />
            <button
              id="api-status-refresh"
              onClick={() => refetch()}
              disabled={isLoading}
              style={{
                padding: '10px 16px',
                borderRadius: 10,
                border: 'none',
                background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
                color: '#fff',
                cursor: isLoading ? 'wait' : 'pointer',
                display: 'flex', alignItems: 'center', gap: 6,
                fontSize: 14, fontWeight: 500,
                opacity: isLoading ? 0.7 : 1,
                transition: 'opacity 0.2s',
              }}
            >
              <RefreshCw size={16} style={{ animation: isLoading ? 'spin 1s linear infinite' : 'none' }} />
              Test
            </button>
          </div>

          {/* Result */}
          <div style={{
            marginTop: 20,
            padding: '1rem 1.25rem',
            borderRadius: 12,
            background: isSuccess
              ? 'rgba(16, 185, 129, 0.08)'
              : isError
                ? 'rgba(239, 68, 68, 0.08)'
                : 'rgba(255,255,255,0.03)',
            border: `1px solid ${isSuccess
              ? 'rgba(16, 185, 129, 0.2)'
              : isError
                ? 'rgba(239, 68, 68, 0.2)'
                : 'rgba(255,255,255,0.06)'
            }`,
          }}>
            {isLoading && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#fbbf24' }}>
                <Activity size={18} />
                <span style={{ fontSize: 14 }}>Tentativo di connessione in corso...</span>
              </div>
            )}

            {isSuccess && (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#34d399', marginBottom: 10 }}>
                  <CheckCircle size={18} />
                  <span style={{ fontSize: 14, fontWeight: 600 }}>Connessione riuscita!</span>
                </div>
                <pre style={{
                  background: 'rgba(0,0,0,0.3)',
                  padding: 12, borderRadius: 8,
                  color: '#94a3b8', fontSize: 12,
                  overflow: 'auto', maxHeight: 200,
                  margin: 0,
                  fontFamily: "'JetBrains Mono', monospace",
                }}>
                  {JSON.stringify(data, null, 2)}
                </pre>
                {dataUpdatedAt && (
                  <p style={{ color: '#64748b', fontSize: 11, marginTop: 8, marginBottom: 0 }}>
                    Ultimo aggiornamento: {new Date(dataUpdatedAt).toLocaleTimeString('it-IT')}
                  </p>
                )}
              </div>
            )}

            {isError && (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#f87171', marginBottom: 8 }}>
                  <XCircle size={18} />
                  <span style={{ fontSize: 14, fontWeight: 600 }}>Connessione fallita</span>
                </div>
                <p style={{ color: '#94a3b8', fontSize: 13, margin: 0 }}>
                  {error?.response
                    ? `Errore ${error.response.status}: ${error.response.statusText}`
                    : error?.message || 'Impossibile raggiungere il server'
                  }
                </p>
              </div>
            )}
          </div>

          {/* Info */}
          <div style={{
            marginTop: 16,
            display: 'flex', alignItems: 'center', gap: 8,
            color: '#64748b', fontSize: 12,
          }}>
            <Wifi size={14} />
            <span>Proxy Vite attivo · le chiamate a <code style={{ color: '#a78bfa' }}>/api/*</code> vengono inoltrate a <code style={{ color: '#a78bfa' }}>localhost:8080</code></span>
          </div>
        </div>
      </div>

      {/* Animations */}
      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
