package ec.edu.epn.fis.aeis.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.epn.fis.aeis.gateway.security.AccessRules;
import ec.edu.epn.fis.aeis.gateway.security.JwtValidator;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Filtro global de autenticación/autorización del gateway (PLAN.md §3.2).
 *
 * <p>Siempre elimina los headers X-User-Id/X-Username/X-User-Roles que vengan
 * del cliente (anti-spoofing), valida el JWT para las rutas protegidas y,
 * si es válido, reenvía la petición agregando esos mismos headers con los
 * datos del token.</p>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        ServerHttpRequest sanitizedRequest = stripSpoofedHeaders(originalRequest);

        String path = originalRequest.getURI().getRawPath();
        HttpMethod method = originalRequest.getMethod();

        if (AccessRules.isPublic(method, path)) {
            return chain.filter(exchange.mutate().request(sanitizedRequest).build());
        }

        String token = extractToken(originalRequest, path);
        if (token == null || token.isBlank()) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Falta el token de autenticación.");
        }

        io.jsonwebtoken.Claims claims;
        try {
            claims = jwtValidator.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Token inválido o expirado.");
        }

        String userId = String.valueOf(claims.get("uid"));
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        if (roles == null) {
            roles = List.of();
        }

        String requiredRole = AccessRules.requiredRole(method, path);
        if (requiredRole != null && !roles.contains(requiredRole)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso.");
        }

        ServerHttpRequest mutatedRequest = sanitizedRequest.mutate()
                .header(HEADER_USER_ID, userId)
                .header(HEADER_USERNAME, username)
                .header(HEADER_USER_ROLES, String.join(",", roles))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private ServerHttpRequest stripSpoofedHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USERNAME);
                    headers.remove(HEADER_USER_ROLES);
                })
                .build();
    }

    private String extractToken(ServerHttpRequest request, String path) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            return request.getQueryParams().getFirst("token");
        }
        return null;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Map.of("error", message));
        } catch (JsonProcessingException e) {
            bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
