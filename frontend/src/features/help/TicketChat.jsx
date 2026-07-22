import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import Spinner from '../../components/Spinner';
import Banner from '../../components/Banner';
import Button from '../../components/Button';
import StatusChip from '../../components/StatusChip';
import { useAuth } from '../../auth/AuthContext';
import { getToken } from '../../auth/storage';
import { useTicket } from './useTicket';
import { useTicketMessages } from './useTicketMessages';
import { closeTicket } from '../../api/helpApi';
import { extractErrorMessage } from '../../api/client';
import { TICKET_STATUS } from '../../utils/constants';
import './TicketChat.css';

export default function TicketChat() {
  const { id } = useParams();
  const { user, isAdmin } = useAuth();
  const { ticket, loading: ticketLoading, error: ticketError } = useTicket(id);
  const { data: history, loading: historyLoading, error: historyError } = useTicketMessages(id);

  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState('');
  const [connected, setConnected] = useState(false);
  const [closed, setClosed] = useState(false);
  const [closing, setClosing] = useState(false);
  const [actionError, setActionError] = useState('');

  const clientRef = useRef(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    if (history) setMessages(history);
  }, [history]);

  useEffect(() => {
    if (ticket) setClosed(ticket.status === 'CLOSED');
  }, [ticket]);

  useEffect(() => {
    const token = getToken();
    const client = new Client({
      brokerURL: `${import.meta.env.VITE_WS_URL}?token=${token}`,
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/tickets/${id}`, (frame) => {
          const body = JSON.parse(frame.body);
          setMessages((prev) => [...prev, body]);
          if (body.ticketClosed) setClosed(true);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setActionError('Error de conexión con el chat.'),
    });
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [id]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  function handleSend(e) {
    e.preventDefault();
    if (!content.trim() || !clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: `/app/tickets/${id}/send`,
      body: JSON.stringify({ content }),
    });
    setContent('');
  }

  async function handleClose() {
    setClosing(true);
    setActionError('');
    try {
      await closeTicket(id);
      setClosed(true);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setClosing(false);
    }
  }

  if (ticketLoading || historyLoading) {
    return (
      <>
        <Navbar />
        <div className="page-content">
          <Spinner label="Cargando ticket..." />
        </div>
      </>
    );
  }

  if (ticketError || historyError || !ticket) {
    return (
      <>
        <Navbar />
        <div className="page-content">
          <Banner type="error">{ticketError || historyError || 'Ticket no encontrado.'}</Banner>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader
          title={ticket.subject}
          subtitle={ticket.rentalRef ? `Ref: ${ticket.rentalRef}` : ticket.description}
          actions={
            <>
              <StatusChip status={closed ? 'CLOSED' : 'OPEN'} statusMap={TICKET_STATUS} />
              {isAdmin && !closed && (
                <Button variant="danger" onClick={handleClose} loading={closing}>
                  Cerrar ticket
                </Button>
              )}
            </>
          }
        />

        <Banner type="error">{actionError}</Banner>

        <Card className="ticket-chat">
          <div className="ticket-chat__messages">
            {messages.length === 0 && (
              <p className="ticket-chat__empty">Aún no hay mensajes. Escribe el primero.</p>
            )}
            {messages.map((m, i) => (
              <div
                key={m.id ?? `event-${i}`}
                className={`ticket-chat__bubble ${
                  m.senderUsername === user.username ? 'ticket-chat__bubble--own' : 'ticket-chat__bubble--other'
                } ${m.ticketClosed ? 'ticket-chat__bubble--system' : ''}`.trim()}
              >
                {!m.ticketClosed && <div className="ticket-chat__meta">{m.senderUsername}</div>}
                <div className="ticket-chat__content">{m.content}</div>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>

          <form className="ticket-chat__input" onSubmit={handleSend}>
            <input
              type="text"
              placeholder={closed ? 'Este ticket está cerrado.' : 'Escribe un mensaje...'}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              disabled={closed || !connected}
              aria-label="Mensaje"
            />
            <button type="submit" disabled={closed || !connected || !content.trim()} aria-label="Enviar">
              <i className="fa-solid fa-paper-plane" />
            </button>
          </form>
        </Card>
      </div>
    </>
  );
}
