package ec.edu.epn.fis.aeis.rental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la respuesta de locker-service a PUT /internal/lockers/{id}/reserve.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReserveResponseDTO {
    private Long id;
    private Integer number;
    private String blockName;
    private Boolean allowCustomRental;
}
