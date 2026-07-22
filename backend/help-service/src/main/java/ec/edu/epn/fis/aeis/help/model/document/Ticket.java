package ec.edu.epn.fis.aeis.help.model.document;

import ec.edu.epn.fis.aeis.help.model.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    private String id;

    private String userId;
    private String username;
    private String subject;
    private String description;
    private String rentalRef;
    private TicketStatus status;
    private Instant createdAt;
}
