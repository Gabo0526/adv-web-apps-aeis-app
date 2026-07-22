package ec.edu.epn.fis.aeis.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExceptionalRentalRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotNull(message = "El casillero es obligatorio")
    private Long lockerId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @NotNull(message = "El monto pagado es obligatorio")
    @PositiveOrZero(message = "El monto pagado debe ser positivo")
    private BigDecimal amountPaid;
}
