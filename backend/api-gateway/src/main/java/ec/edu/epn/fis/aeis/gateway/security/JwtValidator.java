package ec.edu.epn.fis.aeis.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Valida los JWT emitidos por auth-service. El parser se construye solo con
 * el secreto compartido (JWT_SECRET); jjwt infiere el algoritmo desde el
 * propio token y lo valida contra el tipo de clave, sin que el gateway
 * hardcodee HS256.
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
