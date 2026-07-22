package ec.edu.epn.fis.aeis.rental.scheduler;

import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerPreRental;
import ec.edu.epn.fis.aeis.rental.model.enums.PreRentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerPreRentalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Libera casilleros con pre-alquiler PENDING vencido (ver PLAN.md §6.5).
 * Reemplaza la mutación directa de Locker del monolito por una llamada al
 * cliente interno de locker-service.
 */
@Component
@RequiredArgsConstructor
public class LockerPreRentalCleaner {

    private static final Logger log = LoggerFactory.getLogger(LockerPreRentalCleaner.class);

    private final LockerPreRentalRepository preRentalRepository;
    private final LockerServiceClient lockerServiceClient;

    @Scheduled(fixedRate = 60000) // cada 60 segundos
    @Transactional
    public void releaseExpiredPreRentals() {
        LocalDateTime now = LocalDateTime.now();
        List<LockerPreRental> expired = preRentalRepository.findByExpiresAtBeforeAndStatus(now, PreRentalStatus.PENDING);

        for (LockerPreRental preRental : expired) {
            releaseQuietly(preRental.getLockerId());
            preRental.setStatus(PreRentalStatus.EXPIRED);
            preRentalRepository.save(preRental);
        }
    }

    private void releaseQuietly(Long lockerId) {
        try {
            lockerServiceClient.release(lockerId);
        } catch (RuntimeException ex) {
            log.error("No se pudo liberar el casillero {} al expirar el pre-alquiler: {}", lockerId, ex.getMessage());
        }
    }
}
