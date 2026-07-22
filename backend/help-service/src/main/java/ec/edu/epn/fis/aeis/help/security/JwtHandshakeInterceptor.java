package ec.edu.epn.fis.aeis.help.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Decodifica el JWT del query param ?token= durante el handshake WS (el
 * gateway ya validó el token; este servicio solo necesita conocer la
 * identidad del remitente para el chat). Guarda username/rol en los
 * atributos de sesión, disponibles luego como simpSessionAttributes en los
 * métodos @MessageMapping (ver PLAN.md §7.3).
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtValidator jwtValidator;

    public JwtHandshakeInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = jwtValidator.parse(token);
            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            String role = (roles != null && roles.contains("ADMIN")) ? "ADMIN" : "USER";

            attributes.put("username", username);
            attributes.put("role", role);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }
}
