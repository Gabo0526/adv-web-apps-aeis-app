package ec.edu.epn.fis.aeis.locker.repository;

import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LockerRepository extends JpaRepository<Locker, Long> {

    List<Locker> findByLockerBlockId(Long lockerBlockId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Locker l WHERE l.id = :lockerId")
    Optional<Locker> findByIdForUpdate(@Param("lockerId") Long lockerId);
}
