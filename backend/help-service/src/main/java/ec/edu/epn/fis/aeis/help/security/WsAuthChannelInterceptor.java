package ec.edu.epn.fis.aeis.help.security;

import ec.edu.epn.fis.aeis.help.model.document.Ticket;
import ec.edu.epn.fis.aeis.help.repository.TicketRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Autorización del canal STOMP entrante. El broker simple retransmite
 * cualquier SEND dirigido a /topic/** sin pasar por @MessageMapping, y
 * SUBSCRIBE no valida nada por defecto, así que aquí se exige:
 * - SEND solo hacia destinos /app/** (los frames a /topic/** se rechazan).
 * - SUBSCRIBE a /topic/tickets/{id} solo para el dueño del ticket o ADMIN.
 */
@Component
public class WsAuthChannelInterceptor implements ChannelInterceptor {

    private static final String TICKET_TOPIC_PREFIX = "/topic/tickets/";

    private final TicketRepository ticketRepository;

    public WsAuthChannelInterceptor(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case SEND -> validateSend(accessor);
            case SUBSCRIBE -> validateSubscribe(accessor);
            default -> { /* CONNECT, DISCONNECT, etc. pasan */ }
        }
        return message;
    }

    private void validateSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/app/")) {
            throw new MessagingException("Destino no permitido");
        }
    }

    private void validateSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TICKET_TOPIC_PREFIX)) {
            return;
        }

        Map<String, Object> attributes = accessor.getSessionAttributes();
        String username = attributes != null ? (String) attributes.get("username") : null;
        String role = attributes != null ? (String) attributes.get("role") : null;
        if (username == null) {
            throw new MessagingException("No autenticado");
        }
        if ("ADMIN".equals(role)) {
            return;
        }

        String ticketId = destination.substring(TICKET_TOPIC_PREFIX.length());
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null || !username.equals(ticket.getUsername())) {
            throw new MessagingException("Sin acceso al ticket");
        }
    }
}
