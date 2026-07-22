package ec.edu.epn.fis.aeis.rental.scheduler;

import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Libera casilleros de alquileres ACTIVE/PENDING cuya fecha de fin ya pasó
 * (ver PLAN.md §6.5). El estado UNDER_MAINTENANCE se preserva porque
 * locker-service es la única fuente de verdad sobre el estado del casillero.
 */
@Component
@RequiredArgsConstructor
public class LockerRentalCleaner {

    private static final Logger log = LoggerFactory.getLogger(LockerRentalCleaner.class);

    private final LockerRentalRepository lockerRentalRepository;
    private final LockerServiceClient lockerServiceClient;

    @Scheduled(fixedRate = 60000) // cada 60 segundos
    @Transactional
    public void releaseCompletedRentals() {
        LocalDateTime now = LocalDateTime.now();
        List<LockerRental> completed = lockerRentalRepository.findByEndDateBeforeAndStatusIn(
                now, List.of(RentalStatus.ACTIVE, RentalStatus.PENDING));

        for (LockerRental rental : completed) {
            releaseQuietly(rental.getLockerId());
            rental.setStatus(RentalStatus.COMPLETED);
            lockerRentalRepository.save(rental);
        }
    }

    private void releaseQuietly(Long lockerId) {
        try {
            lockerServiceClient.release(lockerId);
        } catch (RuntimeException ex) {
            log.error("No se pudo liberar el casillero {} al completar el alquiler: {}", lockerId, ex.getMessage());
        }
    }
}
