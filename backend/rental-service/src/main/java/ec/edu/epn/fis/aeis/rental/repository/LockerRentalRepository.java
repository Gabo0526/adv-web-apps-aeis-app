package ec.edu.epn.fis.aeis.rental.repository;

import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface LockerRentalRepository extends JpaRepository<LockerRental, Long>, JpaSpecificationExecutor<LockerRental> {

    List<LockerRental> findByUserId(String userId);

    boolean existsByPeriodId(Long periodId);
}
