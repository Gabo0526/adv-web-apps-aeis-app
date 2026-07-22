package ec.edu.epn.fis.aeis.auth.repository;

import ec.edu.epn.fis.aeis.auth.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByIdStartingWith(String idPrefix);
}
