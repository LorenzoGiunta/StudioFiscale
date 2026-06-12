import { useState, useRef, useEffect, useCallback } from 'react';
import { Send, Search, ArrowLeft, Loader2, Wifi, WifiOff } from 'lucide-react';
import { useToast } from '../contexts/ToastContext.jsx';
import { useAuth } from '../contexts/AuthContext.jsx';
import chatService from '../services/ChatService.js';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Componente della chat interna.
 *
 * Combina il caricamento via REST di contatti, storico e messaggi non letti con
 * una connessione WebSocket/STOMP per la ricezione in tempo reale: i messaggi
 * relativi alla conversazione aperta vengono aggiunti subito, gli altri
 * incrementano il contatore dei non letti del rispettivo mittente.
 */
export default function ChatView() {
  const toast = useToast();
  const { user, token } = useAuth();
  const [contacts, setContacts] = useState([]);
  const [activeConv, setActiveConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [unread, setUnread] = useState({}); // { mittenteId: numeroNonLetti }
  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);
  const activeConvRef = useRef(null);

  // Carica contatti disponibili (tutti gli utenti tranne se stesso)
  const loadContacts = useCallback(async () => {
    setLoadingContacts(true);
    try {
      // Il filtraggio dei contatti per ruolo è effettuato dal backend
      const contatti = await chatService.getContatti();
      setContacts(contatti);
    } catch {
      setContacts([]);
    } finally {
      setLoadingContacts(false);
    }
  }, []);

  // Conteggio messaggi non letti per mittente
  const loadUnread = useCallback(async () => {
    try {
      setUnread(await chatService.getNonLetti());
    } catch {
      setUnread({});
    }
  }, []);

  // Mantieni ref aggiornato per evitare stale closure nel WebSocket
  useEffect(() => { activeConvRef.current = activeConv; }, [activeConv]);

  // Connessione WebSocket STOMP
  useEffect(() => {
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${WS_URL}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setWsConnected(true);
        // Sottoscrivi alla coda messaggi personale
        client.subscribe(`/user/queue/messaggi`, (frame) => {
          const msg = JSON.parse(frame.body);
          const conv = activeConvRef.current;
          const inConversazioneAttiva = conv && (msg.mittenteId === conv.id || msg.destinatarioId === conv.id);

          if (inConversazioneAttiva) {
            setMessages(prev => [...prev, { ...msg, mio: msg.mittenteId === user?.id }]);
          } else if (msg.mittenteId !== user?.id) {
            // Messaggio relativo a una conversazione non aperta: aggiorna il contatore
            setUnread(prev => ({ ...prev, [msg.mittenteId]: (prev[msg.mittenteId] || 0) + 1 }));
          }
        });
      },
      onDisconnect: () => setWsConnected(false),
      onStompError: () => setWsConnected(false),
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [token, user]);

  // Carica storico messaggi quando si seleziona una conversazione
  const openConversation = useCallback(async (contact) => {
    setActiveConv(contact);
    setLoadingMessages(true);
    // Azzera subito il badge non letti del contatto
    setUnread(prev => { const n = { ...prev }; delete n[contact.id]; return n; });
    try {
      const data = await chatService.getStorico(contact.id);
      const mapped = data.map(m => ({
        ...m,
        mio: m.isMio(user?.id),
      }));
      setMessages(mapped);
      // Segna i messaggi come letti lato backend (non blocca la UI in caso d'errore)
      chatService.segnaLetti(contact.id).catch(() => {});
    } catch {
      toast.error('Errore nel caricamento dei messaggi');
      setMessages([]);
    } finally {
      setLoadingMessages(false);
    }
  }, [user, toast]);

  useEffect(() => {
    loadContacts();
    loadUnread();
  }, [loadContacts, loadUnread]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = () => {
    if (!newMessage.trim() || !activeConv || !stompClientRef.current?.connected) return;
    const payload = {
      destinatarioId: activeConv.id,
      testo: newMessage.trim(),
    };
    stompClientRef.current.publish({
      destination: '/app/chat.invia',
      body: JSON.stringify(payload),
    });
    // Ottimistic update locale
    setMessages(prev => [...prev, {
      id: Date.now(),
      testo: newMessage.trim(),
      mio: true,
      dataInvio: new Date().toISOString(),
    }]);
    setNewMessage('');
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  const formatTime = (ts) => {
    if (!ts) return '';
    const d = new Date(ts);
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  };

  const filteredContacts = contacts.filter(c => {
    const full = `${c.nome} ${c.cognome}`.toLowerCase();
    return full.includes(searchQuery.toLowerCase());
  });

  const ruoloLabel = (r) => {
    const map = { COMMERCIALISTA: 'Commercialista', COLLABORATORE: 'Collaboratore', CLIENTE: 'Cliente', AMMINISTRATORE: 'Admin' };
    return map[r] || r;
  };

  const initials = (c) => `${c.nome?.[0] || ''}${c.cognome?.[0] || ''}`.toUpperCase();

  return (
    <div className="flex h-[calc(100vh-8rem)] bg-white border border-anthracite-100 rounded-sm overflow-hidden">
      {/* Sidebar contatti */}
      <div className={`w-[340px] border-r border-anthracite-100 flex flex-col shrink-0 ${activeConv ? 'hidden md:flex' : 'flex'}`}>
        <div className="p-4 border-b border-anthracite-100">
          {/* Status WebSocket */}
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-anthracite-500 uppercase tracking-wider">Conversazioni</span>
            <div className="flex items-center gap-1.5">
              {wsConnected
                ? <><Wifi size={13} className="text-emerald-500" /><span className="text-[10px] text-emerald-600">Connesso</span></>
                : <><WifiOff size={13} className="text-red-400" /><span className="text-[10px] text-red-500">Disconnesso</span></>
              }
            </div>
          </div>
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-anthracite-300" />
            <input
              type="text" placeholder="Cerca contatto..."
              value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-sm bg-anthracite-50 border border-anthracite-100 rounded-sm outline-none focus:border-navy-400 transition-colors placeholder:text-anthracite-300"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {loadingContacts ? (
            <div className="flex items-center justify-center py-10">
              <Loader2 size={18} className="animate-spin text-navy-400 mr-2" />
              <span className="text-xs text-anthracite-400">Caricamento contatti...</span>
            </div>
          ) : filteredContacts.length === 0 ? (
            <div className="px-4 py-8 text-center text-xs text-anthracite-400">Nessun contatto disponibile</div>
          ) : (
            filteredContacts.map(c => (
              <button
                key={c.id}
                onClick={() => openConversation(c)}
                className={`w-full flex items-start gap-3 px-4 py-3.5 text-left transition-colors border-b border-anthracite-50 ${
                  activeConv?.id === c.id ? 'bg-navy-50/50' : 'hover:bg-anthracite-50/50'
                }`}
              >
                <div className="w-10 h-10 rounded-sm bg-navy-100 flex items-center justify-center text-sm font-semibold text-navy-600 shrink-0">
                  {initials(c)}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-navy-900 truncate">{c.nome} {c.cognome}</p>
                  <p className="text-[11px] text-amber-600 font-medium">{ruoloLabel(c.ruolo)}</p>
                </div>
                {unread[c.id] > 0 && (
                  <span className="ml-2 shrink-0 min-w-[20px] h-5 px-1.5 flex items-center justify-center text-[11px] font-bold text-white bg-amber-500 rounded-full">
                    {unread[c.id]}
                  </span>
                )}
              </button>
            ))
          )}
        </div>
      </div>

      {/* Area chat */}
      {activeConv ? (
        <div className="flex-1 flex flex-col">
          {/* Header */}
          <div className="h-16 px-5 flex items-center gap-3 border-b border-anthracite-100 shrink-0">
            <button onClick={() => setActiveConv(null)} className="md:hidden p-1.5 text-anthracite-400 hover:text-navy-900">
              <ArrowLeft size={18} />
            </button>
            <div className="w-9 h-9 rounded-sm bg-navy-100 flex items-center justify-center text-sm font-semibold text-navy-600">
              {initials(activeConv)}
            </div>
            <div>
              <p className="text-sm font-medium text-navy-900">{activeConv.nome} {activeConv.cognome}</p>
              <p className="text-[11px] text-anthracite-400">{ruoloLabel(activeConv.ruolo)}</p>
            </div>
          </div>

          {/* Messaggi */}
          <div className="flex-1 overflow-y-auto px-5 py-4 bg-anthracite-50/30">
            {loadingMessages ? (
              <div className="flex items-center justify-center h-full">
                <Loader2 size={20} className="animate-spin text-navy-400 mr-2" />
                <span className="text-sm text-anthracite-400">Caricamento messaggi...</span>
              </div>
            ) : messages.length === 0 ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <Send size={28} strokeWidth={1.5} className="text-anthracite-200 mx-auto mb-2" />
                  <p className="text-xs text-anthracite-400">Nessun messaggio. Inizia la conversazione!</p>
                </div>
              </div>
            ) : (
              <div className="space-y-3 max-w-3xl mx-auto">
                {messages.map(msg => (
                  <div key={msg.id} className={`flex ${msg.mio ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[70%] px-4 py-2.5 text-sm leading-relaxed ${
                      msg.mio
                        ? 'bg-navy-900 text-white rounded-sm rounded-br-none'
                        : 'bg-white text-navy-800 border border-anthracite-100 rounded-sm rounded-bl-none'
                    }`}>
                      <p>{msg.testo || msg.text}</p>
                      <p className={`text-[10px] mt-1 text-right ${msg.mio ? 'text-navy-300' : 'text-anthracite-400'}`}>
                        {formatTime(msg.dataInvio || msg.timestamp)}
                      </p>
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Input */}
          <div className="px-5 py-3 border-t border-anthracite-100 bg-white">
            <div className="flex items-end gap-2 max-w-3xl mx-auto">
              <div className="flex-1 relative">
                <textarea
                  value={newMessage} onChange={(e) => setNewMessage(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder={wsConnected ? "Scrivi un messaggio..." : "In attesa di connessione..."}
                  disabled={!wsConnected}
                  rows={1}
                  className="w-full px-4 py-2.5 text-sm bg-anthracite-50 border border-anthracite-100 rounded-sm outline-none focus:border-navy-400 transition-colors resize-none placeholder:text-anthracite-300 disabled:opacity-50"
                />
              </div>
              <button
                onClick={handleSend}
                disabled={!newMessage.trim() || !wsConnected}
                className="p-2.5 bg-navy-900 text-white rounded-sm hover:bg-navy-800 transition-colors disabled:opacity-30 disabled:cursor-not-allowed shrink-0"
              >
                <Send size={18} strokeWidth={1.75} />
              </button>
            </div>
            {!wsConnected && (
              <p className="text-[10px] text-red-400 mt-1 text-center">WebSocket disconnesso — i messaggi non possono essere inviati</p>
            )}
          </div>
        </div>
      ) : (
        <div className="flex-1 flex items-center justify-center bg-anthracite-50/30">
          <div className="text-center">
            <div className="w-16 h-16 rounded-sm bg-anthracite-100 flex items-center justify-center mx-auto mb-4">
              <Send size={24} strokeWidth={1.5} className="text-anthracite-300" />
            </div>
            <p className="text-sm font-medium text-navy-900 mb-1">Seleziona una conversazione</p>
            <p className="text-xs text-anthracite-400">Scegli un contatto dalla lista per iniziare</p>
          </div>
        </div>
      )}
    </div>
  );
}
