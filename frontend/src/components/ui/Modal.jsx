import { useEffect, useRef } from 'react';
import { X } from 'lucide-react';

/**
 * Finestra modale riutilizzabile.
 *
 * Mostra il contenuto sovrapposto alla pagina con titolo e dimensione
 * configurabili, gestendone la chiusura tramite pulsante o interazione esterna.
 */
export default function Modal({ isOpen, onClose, title, children, size = 'md' }) {
  const overlayRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => { document.body.style.overflow = ''; };
  }, [isOpen]);

  if (!isOpen) return null;

  const sizes = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  };

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-50 flex items-start justify-center pt-[10vh] px-4 bg-navy-950/60 backdrop-blur-sm animate-fade-in"
      onClick={(e) => { if (e.target === overlayRef.current) onClose(); }}
    >
      <div className={`${sizes[size]} w-full bg-white rounded-sm shadow-xl animate-slide-up`}>
        <div className="flex items-center justify-between px-6 py-4 border-b border-anthracite-100">
          <h2 className="text-base font-semibold text-navy-900">{title}</h2>
          <button onClick={onClose} className="p-1.5 text-anthracite-400 hover:text-navy-900 hover:bg-anthracite-50 rounded-sm transition-colors">
            <X size={18} />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}
