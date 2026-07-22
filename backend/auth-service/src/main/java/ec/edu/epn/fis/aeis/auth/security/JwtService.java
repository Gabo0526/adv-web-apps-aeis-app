package ec.edu.epn.fis.aeis.auth.security;

import ec.edu.epn.fis.aeis.auth.model.entity.Role;
import ec.edu.epn.fis.aeis.auth.model.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationHours;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-hours}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generateToken(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("name", user.getName() + " " + user.getLastName())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
