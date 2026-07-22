package ec.edu.epn.fis.aeis.help.model.document;

import ec.edu.epn.fis.aeis.help.model.enums.SenderRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    private String ticketId;
    private String senderUsername;
    private SenderRole senderRole;
    private String content;
    private Instant sentAt;
}
