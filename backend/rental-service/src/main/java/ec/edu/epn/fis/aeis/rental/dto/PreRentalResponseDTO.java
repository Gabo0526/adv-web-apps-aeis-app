package ec.edu.epn.fis.aeis.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Datos que el frontend necesita para renderizar la Cajita de Pagos de PayPhone
 * (ver PLAN.md §6.3 y §8.4). amountCents va en centavos.
 */
@Getter
@AllArgsConstructor
public class PreRentalResponseDTO {
    private Long preRentalId;
    private String clientTransactionId;
    private Long amountCents;
    private String reference;
    private String payphoneToken;
    private String payphoneStoreId;
}
