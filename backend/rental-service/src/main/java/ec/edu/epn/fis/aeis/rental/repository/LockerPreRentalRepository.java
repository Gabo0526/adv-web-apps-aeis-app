package ec.edu.epn.fis.aeis.rental.repository;

import ec.edu.epn.fis.aeis.rental.model.entity.LockerPreRental;
import ec.edu.epn.fis.aeis.rental.model.enums.PreRentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LockerPreRentalRepository extends JpaRepository<LockerPreRental, Long> {

    Optional<LockerPreRental> findByPayPhoneClientTransactionId(String payPhoneClientTransactionId);

    List<LockerPreRental> findByExpiresAtBeforeAndStatus(LocalDateTime expiresAt, PreRentalStatus status);
}
