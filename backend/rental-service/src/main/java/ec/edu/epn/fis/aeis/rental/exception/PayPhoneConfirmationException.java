package ec.edu.epn.fis.aeis.rental.exception;

/**
 * Lanzada cuando la API de confirmación de PayPhone no responde o responde con error.
 * Ver PLAN.md §6.3.
 */
public class PayPhoneConfirmationException extends RuntimeException {
    public PayPhoneConfirmationException(String message) {
        super(message);
    }
}
