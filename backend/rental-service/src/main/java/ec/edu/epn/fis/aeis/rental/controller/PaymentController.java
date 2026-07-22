package ec.edu.epn.fis.aeis.rental.controller;

import ec.edu.epn.fis.aeis.rental.dto.PaymentConfirmResponseDTO;
import ec.edu.epn.fis.aeis.rental.service.LockerPreRentalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público (ver PLAN.md §3.2) al que el frontend llama tras el
 * redirect de PayPhone en /payment/result.
 */
@RestController
@RequestMapping("/payments/payphone")
public class PaymentController {

    private final LockerPreRentalService lockerPreRentalService;

    public PaymentController(LockerPreRentalService lockerPreRentalService) {
        this.lockerPreRentalService = lockerPreRentalService;
    }

    @GetMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponseDTO> confirmPayment(
            @RequestParam("id") Long id,
            @RequestParam("clientTransactionId") String clientTransactionId) {
        boolean success = lockerPreRentalService.confirmPreRental(id, clientTransactionId);
        String message = success
                ? "¡Tu pago ha sido confirmado exitosamente!"
                : "Tu pago fue rechazado. Por favor, verifica tu información de pago e inténtalo de nuevo.";
        return ResponseEntity.ok(new PaymentConfirmResponseDTO(success, message));
    }
}
