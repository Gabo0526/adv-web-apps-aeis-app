package ec.edu.epn.fis.aeis.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PreRentalRequestDTO {

    @NotNull(message = "El casillero es obligatorio")
    private Long lockerId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
