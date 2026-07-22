package ec.edu.epn.fis.aeis.auth.repository;

import ec.edu.epn.fis.aeis.auth.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
