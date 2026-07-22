package ec.edu.epn.fis.aeis.rental.exception;

/**
 * Lanzada por los fallbacks de Resilience4j cuando locker-service o auth-service
 * no responden (circuito abierto, timeout o error de red). Ver PLAN.md §6.2.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
