package ec.edu.epn.fis.aeis.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentConfirmResponseDTO {
    private boolean success;
    private String message;
}
