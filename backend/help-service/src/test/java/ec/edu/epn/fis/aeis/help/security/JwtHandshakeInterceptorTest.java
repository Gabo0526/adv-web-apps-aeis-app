package ec.edu.epn.fis.aeis.help.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtHandshakeInterceptorTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";
    private final JwtValidator jwtValidator = new JwtValidator(SECRET);
    private final JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtValidator);

    private String token(List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("jdoe")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    @Test
    void whenValidAdminToken_thenStoresUsernameAndAdminRole() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setQueryString("token=" + token(List.of("USER", "ADMIN")));
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertTrue(result);
        assertEquals("jdoe", attributes.get("username"));
        assertEquals("ADMIN", attributes.get("role"));
    }

    @Test
    void whenValidUserToken_thenStoresUserRole() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setQueryString("token=" + token(List.of("USER")));
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertTrue(result);
        assertEquals("USER", attributes.get("role"));
    }

    @Test
    void whenNoToken_thenRejectsHandshake() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(mockResponse);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertFalse(result);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
    }

    @Test
    void whenInvalidToken_thenRejectsHandshake() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setQueryString("token=not-a-valid-jwt");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, null, attributes);

        assertFalse(result);
    }
}
