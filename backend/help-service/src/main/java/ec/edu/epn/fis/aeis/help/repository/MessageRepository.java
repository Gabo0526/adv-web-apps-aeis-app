package ec.edu.epn.fis.aeis.help.repository;

import ec.edu.epn.fis.aeis.help.model.document.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByTicketIdOrderBySentAtAsc(String ticketId);
}
