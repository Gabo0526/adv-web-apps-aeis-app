package ec.edu.epn.fis.aeis.auth.repository;

import ec.edu.epn.fis.aeis.auth.model.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
}
