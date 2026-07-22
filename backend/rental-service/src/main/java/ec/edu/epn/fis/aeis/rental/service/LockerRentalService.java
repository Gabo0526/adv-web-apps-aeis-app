package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.client.AuthServiceClient;
import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.dto.ExceptionalRentalRequestDTO;
import ec.edu.epn.fis.aeis.rental.dto.InternalUserDTO;
import ec.edu.epn.fis.aeis.rental.dto.LockerRentalDTO;
import ec.edu.epn.fis.aeis.rental.dto.LockerRentalFilterDTO;
import ec.edu.epn.fis.aeis.rental.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.rental.exception.NoActivePeriodException;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import ec.edu.epn.fis.aeis.rental.specification.LockerRentalSpecification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LockerRentalService {

    private static final Logger log = LoggerFactory.getLogger(LockerRentalService.class);

    private final LockerRentalRepository lockerRentalRepository;
    private final PeriodService periodService;
    private final AuthServiceClient authServiceClient;
    private final LockerServiceClient lockerServiceClient;
    private final RentalValidator rentalValidator;

    public LockerRental createExceptionalRental(ExceptionalRentalRequestDTO request) {
        Period activePeriod = periodService.getActivePeriod()
                .orElseThrow(() -> new NoActivePeriodException("No hay un período activo para procesar el alquiler"));

        InternalUserDTO user = authServiceClient.findByUsername(request.getUsername());

        ReserveResponseDTO reserved = lockerServiceClient.reserve(request.getLockerId());

        try {
            boolean customEndDateProvided = request.getEndDate() != null;
            rentalValidator.validateCustomRentalSupport(Boolean.TRUE.equals(reserved.getAllowCustomRental()), customEndDateProvided);

            LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now();
            LocalDateTime endDate;
            if (customEndDateProvided) {
                rentalValidator.validateRentalDates(startDate, request.getEndDate());
                // Igual que en el flujo normal: la renta vence al terminar el día de fin.
                endDate = request.getEndDate().toLocalDate().atTime(23, 59, 59);
            } else {
                endDate = activePeriod.getEndDate();
            }

            lockerServiceClient.occupy(reserved.getId());

            LockerRental rental = new LockerRental();
            rental.setUserId(user.getId());
            rental.setUsername(user.getUsername());
            rental.setUserFullName(user.getName() + " " + user.getLastName());
            rental.setLockerId(reserved.getId());
            rental.setLockerNumber(reserved.getNumber());
            rental.setBlockName(reserved.getBlockName());
            rental.setPeriod(activePeriod);
            rental.setStartDate(startDate);
            rental.setEndDate(endDate);
            rental.setAmountPaid(request.getAmountPaid());
            rental.setStatus(RentalStatus.ACTIVE);

            return lockerRentalRepository.save(rental);
        } catch (RuntimeException ex) {
            compensateReserve(reserved.getId());
            throw ex;
        }
    }

    private void compensateReserve(Long lockerId) {
        try {
            lockerServiceClient.release(lockerId);
        } catch (RuntimeException releaseEx) {
            log.error("No se pudo liberar el casillero {} tras un error en la renta: {}", lockerId, releaseEx.getMessage());
        }
    }

    public Page<LockerRentalDTO> getAllLockerRentals(Pageable pageable) {
        return lockerRentalRepository.findAll(pageable).map(LockerRentalDTO::new);
    }

    public Page<LockerRentalDTO> getFilteredLockerRentals(LockerRentalFilterDTO filters, Pageable pageable) {
        var spec = LockerRentalSpecification.build(filters);
        return lockerRentalRepository.findAll(spec, pageable).map(LockerRentalDTO::new);
    }

    public List<LockerRentalDTO> getMine(String userId) {
        return lockerRentalRepository.findByUserId(userId).stream()
                .map(LockerRentalDTO::new)
                .toList();
    }
}
