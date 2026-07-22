package ec.edu.epn.fis.aeis.help.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Valida los JWT emitidos por auth-service, usados aquí para identificar al
 * remitente del handshake WebSocket (ver PLAN.md §7.3).
 */
@Component
public class JwtValidator {

    private final JwtParser parser;

    public JwtValidator(@Value("${jwt.secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    public Claims parse(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
