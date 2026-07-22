package ec.edu.epn.fis.aeis.help.dto;

import ec.edu.epn.fis.aeis.help.model.document.Ticket;
import ec.edu.epn.fis.aeis.help.model.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TicketDTO {

    private String id;
    private String userId;
    private String username;
    private String subject;
    private String description;
    private String rentalRef;
    private TicketStatus status;
    private Instant createdAt;

    public TicketDTO(Ticket ticket) {
        this.id = ticket.getId();
        this.userId = ticket.getUserId();
        this.username = ticket.getUsername();
        this.subject = ticket.getSubject();
        this.description = ticket.getDescription();
        this.rentalRef = ticket.getRentalRef();
        this.status = ticket.getStatus();
        this.createdAt = ticket.getCreatedAt();
    }
}
