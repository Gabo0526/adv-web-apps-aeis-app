package ec.edu.epn.fis.aeis.locker.repository;

import ec.edu.epn.fis.aeis.locker.model.entity.LockerBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LockerBlockRepository extends JpaRepository<LockerBlock, Long> {

    @Query("SELECT DISTINCT b FROM LockerBlock b LEFT JOIN FETCH b.lockers ORDER BY b.id")
    List<LockerBlock> findAllWithLockers();
}
