package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.dto.PeriodDTO;
import ec.edu.epn.fis.aeis.rental.dto.PeriodRequestDTO;
import ec.edu.epn.fis.aeis.rental.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import ec.edu.epn.fis.aeis.rental.repository.PeriodRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PeriodService {

    private final PeriodRepository periodRepository;
    private final LockerRentalRepository lockerRentalRepository;

    public List<PeriodDTO> findAllDTO() {
        return periodRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"))
                .stream()
                .map(PeriodDTO::new)
                .toList();
    }

    public Optional<Period> getActivePeriod() {
        return periodRepository.findByActiveTrue();
    }

    public Period saveNewPeriod(PeriodRequestDTO dto) {
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        Period period = new Period();
        period.setName(dto.getName());
        period.setStartDate(dto.getStartDate());
        period.setEndDate(dto.getEndDate());
        period.setActive(false);

        return periodRepository.save(period);
    }

    @Transactional
    public Period updatePeriod(Long id, PeriodRequestDTO dto) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Período no encontrado"));

        if (lockerRentalRepository.existsByPeriodId(id)) {
            throw new IllegalStateException("No se puede editar un período que ya tiene alquileres asociados");
        }

        validateDateRange(dto.getStartDate(), dto.getEndDate());

        period.setName(dto.getName());
        period.setStartDate(dto.getStartDate());
        period.setEndDate(dto.getEndDate());

        return periodRepository.save(period);
    }

    @Transactional
    public Period activatePeriod(Long id) {
        periodRepository.findByActiveTrue().ifPresent(p -> {
            p.setActive(false);
            periodRepository.save(p);
        });

        Period periodToActivate = periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Período no encontrado"));

        periodToActivate.setActive(true);
        return periodRepository.save(periodToActivate);
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }
}
