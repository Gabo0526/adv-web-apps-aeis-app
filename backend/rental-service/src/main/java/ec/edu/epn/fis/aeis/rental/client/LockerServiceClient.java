package ec.edu.epn.fis.aeis.rental.client;

import ec.edu.epn.fis.aeis.rental.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.rental.exception.LockerConflictException;
import ec.edu.epn.fis.aeis.rental.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.rental.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente interno hacia locker-service (/internal/lockers/**). Protegido con
 * Circuit Breaker Resilience4j: si el servicio no responde, el fallback
 * lanza ServiceUnavailableException, que el GlobalExceptionHandler mapea a 503
 * (ver PLAN.md §6.2).
 */
@Component
public class LockerServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LockerServiceClient.class);
    private static final String CB_NAME = "lockerService";

    private final RestClient restClient;

    public LockerServiceClient(RestClient lockerServiceRestClient) {
        this.restClient = lockerServiceRestClient;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "reserveFallback")
    public ReserveResponseDTO reserve(Long lockerId) {
        return restClient.put()
                .uri("/internal/lockers/{id}/reserve", lockerId)
                .retrieve()
                .onStatus(this::isConflict, (req, res) -> {
                    throw new LockerConflictException("El casillero no está disponible para reservar");
                })
                .onStatus(this::isNotFound, (req, res) -> {
                    throw new ResourceNotFoundException("Casillero no encontrado con ID: " + lockerId);
                })
                .body(ReserveResponseDTO.class);
    }

    private ReserveResponseDTO reserveFallback(Long lockerId, Exception ex) {
        return rethrowOrFail(ex);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "occupyFallback")
    public void occupy(Long lockerId) {
        restClient.put()
                .uri("/internal/lockers/{id}/occupy", lockerId)
                .retrieve()
                .onStatus(this::isConflict, (req, res) -> {
                    throw new LockerConflictException("El casillero no está pendiente de asignación");
                })
                .toBodilessEntity();
    }

    private void occupyFallback(Long lockerId, Exception ex) {
        rethrowOrFail(ex);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "releaseFallback")
    public void release(Long lockerId) {
        restClient.put()
                .uri("/internal/lockers/{id}/release", lockerId)
                .retrieve()
                .toBodilessEntity();
    }

    private void releaseFallback(Long lockerId, Exception ex) {
        rethrowOrFail(ex);
    }

    private <T> T rethrowOrFail(Exception ex) {
        if (ex instanceof LockerConflictException || ex instanceof ResourceNotFoundException) {
            throw (RuntimeException) ex;
        }
        log.error("locker-service no disponible: {}", ex.getMessage());
        throw new ServiceUnavailableException("Servicio de casilleros no disponible, intenta más tarde");
    }

    private boolean isConflict(HttpStatusCode status) {
        return status.value() == 409;
    }

    private boolean isNotFound(HttpStatusCode status) {
        return status.value() == 404;
    }
}
