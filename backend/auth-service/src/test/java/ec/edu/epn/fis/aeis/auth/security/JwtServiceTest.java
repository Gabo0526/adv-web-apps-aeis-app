package ec.edu.epn.fis.aeis.auth.security;

import ec.edu.epn.fis.aeis.auth.model.entity.Role;
import ec.edu.epn.fis.aeis.auth.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 8);
    }

    @Test
    void whenGenerateToken_thenContainsExpectedClaims() {
        User user = new User();
        user.setId("1234567890");
        user.setUsername("jdoe");
        user.setName("John");
        user.setLastName("Doe");
        user.setRoles(Set.of(new Role("USER"), new Role("ADMIN")));

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals("jdoe", claims.getSubject());
        assertEquals("1234567890", claims.get("uid"));
        assertEquals("John Doe", claims.get("name"));
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertTrue(roles.containsAll(List.of("USER", "ADMIN")));
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }
}
