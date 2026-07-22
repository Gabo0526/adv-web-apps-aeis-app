package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.client.AuthServiceClient;
import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.dto.InternalUserDTO;
import ec.edu.epn.fis.aeis.rental.dto.PayPhoneAPIResponseDTO;
import ec.edu.epn.fis.aeis.rental.dto.PayPhoneConfirmationDTO;
import ec.edu.epn.fis.aeis.rental.dto.PreRentalResponseDTO;
import ec.edu.epn.fis.aeis.rental.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.rental.exception.NoActivePeriodException;
import ec.edu.epn.fis.aeis.rental.exception.PayPhoneConfirmationException;
import ec.edu.epn.fis.aeis.rental.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerPreRental;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.model.enums.LockerRentalSetting;
import ec.edu.epn.fis.aeis.rental.model.enums.PreRentalStatus;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerPreRentalRepository;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flujo de renta con PayPhone (ver PLAN.md §6.3): pre-alquiler -> Cajita de Pagos ->
 * confirmación. Réplica de LockerPreRentalService del monolito adaptada a los
 * clientes internos de locker-service/auth-service.
 */
@Service
@RequiredArgsConstructor
public class LockerPreRentalService {

    private static final Logger log = LoggerFactory.getLogger(LockerPreRentalService.class);
    private static final long PRE_RENTAL_EXPIRATION_MINUTES = 10;
    private static final int OCCUPY_MAX_ATTEMPTS = 3;

    private final LockerPreRentalRepository preRentalRepository;
    private final LockerRentalRepository lockerRentalRepository;
    private final PeriodService periodService;
    private final AuthServiceClient authServiceClient;
    private final LockerServiceClient lockerServiceClient;
    private final RentalValidator rentalValidator;
    private final RentalCalculator rentalCalculator;
    private final PayPhoneService payPhoneService;

    @Value("${payphone.token}")
    private String payPhoneToken;
    @Value("${payphone.store-id}")
    private String payPhoneStoreId;

    @Transactional
    public PreRentalResponseDTO createPreRental(String userId, String username, Long lockerId,
                                                 LocalDateTime startDate, LocalDateTime endDate) {
        ReserveResponseDTO reserved = lockerServiceClient.reserve(lockerId);

        try {
            Period activePeriod = periodService.getActivePeriod()
                    .orElseThrow(() -> new NoActivePeriodException("No hay un período activo para procesar el alquiler"));

            LocalDateTime start = startDate != null ? startDate : LocalDateTime.now();
            boolean customEndDateProvided = endDate != null;
            rentalValidator.validateCustomRentalSupport(Boolean.TRUE.equals(reserved.getAllowCustomRental()), customEndDateProvided);

            LocalDateTime end;
            BigDecimal amountToPay;
            if (customEndDateProvided) {
                rentalValidator.validateRentalDates(start, endDate);
                end = endDate;
                amountToPay = rentalCalculator.calculateCustomRentalAmount(start, end);
            } else {
                end = activePeriod.getEndDate();
                amountToPay = LockerRentalSetting.PERIOD_RENT_PRICE.getValueAsBigDecimal();
            }

            LocalDateTime now = LocalDateTime.now();
            String clientTransactionId = UUID.randomUUID().toString();

            LockerPreRental preRental = new LockerPreRental();
            preRental.setUserId(userId);
            preRental.setUsername(username);
            preRental.setLockerId(reserved.getId());
            preRental.setLockerNumber(reserved.getNumber());
            preRental.setBlockName(reserved.getBlockName());
            preRental.setAllowCustomRental(Boolean.TRUE.equals(reserved.getAllowCustomRental()));
            preRental.setCreatedAt(now);
            preRental.setExpiresAt(now.plusMinutes(PRE_RENTAL_EXPIRATION_MINUTES));
            preRental.setStartDate(start);
            preRental.setEndDate(end);
            preRental.setAmountToPay(amountToPay);
            preRental.setPayPhoneClientTransactionId(clientTransactionId);
            preRental.setStatus(PreRentalStatus.PENDING);

            LockerPreRental saved = preRentalRepository.save(preRental);

            long amountCents = saved.getAmountToPay().multiply(BigDecimal.valueOf(100)).longValueExact();
            String reference = "Alquiler de Casillero #" + saved.getLockerNumber() + " del bloque " + saved.getBlockName();

            return new PreRentalResponseDTO(saved.getId(), clientTransactionId, amountCents, reference,
                    payPhoneToken, payPhoneStoreId);
        } catch (RuntimeException ex) {
            compensateReserve(reserved.getId());
            throw ex;
        }
    }

    @Transactional
    public boolean confirmPreRental(Long payPhoneId, String clientTransactionId) {
        LockerPreRental preRental = preRentalRepository.findByPayPhoneClientTransactionId(clientTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un pre-alquiler para el ID proporcionado."));

        if (preRental.getStatus() != PreRentalStatus.PENDING) {
            throw new IllegalStateException("El pre-alquiler no está en un estado válido para confirmar.");
        }

        PayPhoneAPIResponseDTO payPhoneResponse = payPhoneService.confirmPayPhonePayment(
                new PayPhoneConfirmationDTO(payPhoneId, clientTransactionId));

        if (payPhoneResponse == null) {
            throw new PayPhoneConfirmationException("Ocurrió un error al consumir la API de confirmación de PayPhone.");
        }

        preRental.setPayPhoneTransactionId(payPhoneResponse.getTransactionId());

        boolean paid = Boolean.TRUE.equals(payPhoneResponse.isPaid());
        if (paid) {
            occupyWithRetry(preRental.getLockerId());
            lockerRentalRepository.save(buildRentalFromPreRental(preRental));
            preRental.setStatus(PreRentalStatus.COMPLETED);
        } else {
            releaseQuietly(preRental.getLockerId());
            preRental.setStatus(PreRentalStatus.CANCELLED);
            log.warn("El pago fue rechazado por PayPhone para el pre-alquiler ID: {}", preRental.getId());
        }

        preRentalRepository.save(preRental);
        return paid;
    }

    private LockerRental buildRentalFromPreRental(LockerPreRental preRental) {
        String userFullName = null;
        try {
            InternalUserDTO user = authServiceClient.findByUsername(preRental.getUsername());
            userFullName = user.getName() + " " + user.getLastName();
        } catch (RuntimeException ex) {
            log.error("No se pudo obtener el nombre completo del usuario {} para el alquiler: {}",
                    preRental.getUsername(), ex.getMessage());
        }

        LockerRental rental = new LockerRental();
        rental.setUserId(preRental.getUserId());
        rental.setUsername(preRental.getUsername());
        rental.setUserFullName(userFullName);
        rental.setLockerId(preRental.getLockerId());
        rental.setLockerNumber(preRental.getLockerNumber());
        rental.setBlockName(preRental.getBlockName());
        rental.setLockerPreRental(preRental);
        rental.setPeriod(periodService.getActivePeriod().orElse(null));
        rental.setStartDate(preRental.getStartDate());
        rental.setEndDate(preRental.getEndDate());
        rental.setAmountPaid(preRental.getAmountToPay());
        rental.setStatus(RentalStatus.ACTIVE);
        return rental;
    }

    private void occupyWithRetry(Long lockerId) {
        for (int attempt = 1; attempt <= OCCUPY_MAX_ATTEMPTS; attempt++) {
            try {
                lockerServiceClient.occupy(lockerId);
                return;
            } catch (RuntimeException ex) {
                if (attempt == OCCUPY_MAX_ATTEMPTS) {
                    log.error("No se pudo marcar como OCCUPIED el casillero {} tras el pago confirmado, "
                            + "después de {} intentos: {}", lockerId, OCCUPY_MAX_ATTEMPTS, ex.getMessage());
                }
            }
        }
    }

    private void releaseQuietly(Long lockerId) {
        try {
            lockerServiceClient.release(lockerId);
        } catch (RuntimeException ex) {
            log.error("No se pudo liberar el casillero {} tras el rechazo del pago: {}", lockerId, ex.getMessage());
        }
    }

    private void compensateReserve(Long lockerId) {
        try {
            lockerServiceClient.release(lockerId);
        } catch (RuntimeException ex) {
            log.error("No se pudo liberar el casillero {} tras un error en la creación del pre-alquiler: {}",
                    lockerId, ex.getMessage());
        }
    }
}
