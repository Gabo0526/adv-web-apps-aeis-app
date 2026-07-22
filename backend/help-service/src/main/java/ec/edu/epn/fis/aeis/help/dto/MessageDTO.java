package ec.edu.epn.fis.aeis.help.dto;

import ec.edu.epn.fis.aeis.help.model.document.Message;
import ec.edu.epn.fis.aeis.help.model.enums.SenderRole;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Representa un mensaje persistido. Cuando {@code ticketClosed} es true, el
 * objeto es un evento transitorio (no se guarda en Mongo) que el servidor
 * publica en /topic/tickets/{id} para avisar en tiempo real que un ADMIN
 * cerró el ticket (ver TicketService#closeTicket).
 */
@Getter
@Setter
public class MessageDTO {

    private String id;
    private String ticketId;
    private String senderUsername;
    private SenderRole senderRole;
    private String content;
    private Instant sentAt;
    private boolean ticketClosed;

    public MessageDTO(Message message) {
        this.id = message.getId();
        this.ticketId = message.getTicketId();
        this.senderUsername = message.getSenderUsername();
        this.senderRole = message.getSenderRole();
        this.content = message.getContent();
        this.sentAt = message.getSentAt();
        this.ticketClosed = false;
    }

    public static MessageDTO closedEvent(String ticketId, String closedByUsername) {
        MessageDTO dto = new MessageDTO();
        dto.ticketId = ticketId;
        dto.senderUsername = closedByUsername;
        dto.senderRole = SenderRole.ADMIN;
        dto.content = "El ticket fue cerrado.";
        dto.sentAt = Instant.now();
        dto.ticketClosed = true;
        return dto;
    }

    private MessageDTO() {
    }
}
