package ec.edu.epn.fis.aeis.rental.repository;

import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodRepository extends JpaRepository<Period, Long> {

    Optional<Period> findByActiveTrue();
}
