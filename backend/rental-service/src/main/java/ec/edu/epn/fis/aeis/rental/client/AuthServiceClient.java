package ec.edu.epn.fis.aeis.rental.client;

import ec.edu.epn.fis.aeis.rental.dto.InternalUserDTO;
import ec.edu.epn.fis.aeis.rental.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.rental.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente interno hacia auth-service (/internal/users/**). Protegido con
 * Circuit Breaker Resilience4j (ver PLAN.md §6.2).
 */
@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    private static final String CB_NAME = "authService";

    private final RestClient restClient;

    public AuthServiceClient(RestClient authServiceRestClient) {
        this.restClient = authServiceRestClient;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findByUsernameFallback")
    public InternalUserDTO findByUsername(String username) {
        return restClient.get()
                .uri("/internal/users/{username}", username)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, res) -> {
                    throw new ResourceNotFoundException("Usuario no encontrado: " + username);
                })
                .body(InternalUserDTO.class);
    }

    private InternalUserDTO findByUsernameFallback(String username, Exception ex) {
        if (ex instanceof ResourceNotFoundException) {
            throw (ResourceNotFoundException) ex;
        }
        log.error("auth-service no disponible: {}", ex.getMessage());
        throw new ServiceUnavailableException("Servicio de usuarios no disponible, intenta más tarde");
    }
}
