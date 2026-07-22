package ec.edu.epn.fis.aeis.help.repository;

import ec.edu.epn.fis.aeis.help.model.document.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TicketRepository extends MongoRepository<Ticket, String> {

    List<Ticket> findByUserId(String userId);
}
