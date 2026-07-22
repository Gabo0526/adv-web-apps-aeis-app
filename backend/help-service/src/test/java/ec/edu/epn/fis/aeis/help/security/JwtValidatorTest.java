package ec.edu.epn.fis.aeis.help.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    private String tokenSignedWith(String secret, Date expiration, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("jdoe")
                .claim("uid", "1234567890")
                .claim("name", "John Doe")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    @Test
    void whenTokenSignedWithSameSecret_thenParsesClaims() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = tokenSignedWith(SECRET, Date.from(Instant.now().plus(1, ChronoUnit.HOURS)), List.of("USER"));

        Claims claims = validator.parse(token);

        assertEquals("jdoe", claims.getSubject());
        assertEquals(List.of("USER"), claims.get("roles", List.class));
    }

    @Test
    void whenTokenSignedWithDifferentSecret_thenThrows() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = tokenSignedWith("another-secret-another-secret-another-secret",
                Date.from(Instant.now().plus(1, ChronoUnit.HOURS)), List.of("USER"));

        assertThrows(SignatureException.class, () -> validator.parse(token));
    }

    @Test
    void whenTokenExpired_thenThrows() {
        JwtValidator validator = new JwtValidator(SECRET);
        String token = tokenSignedWith(SECRET, Date.from(Instant.now().minus(1, ChronoUnit.HOURS)), List.of("USER"));

        assertThrows(ExpiredJwtException.class, () -> validator.parse(token));
    }
}
