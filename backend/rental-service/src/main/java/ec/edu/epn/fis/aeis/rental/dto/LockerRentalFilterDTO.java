package ec.edu.epn.fis.aeis.rental.dto;

import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LockerRentalFilterDTO {
    private String username;
    private String blockName;
    private RentalStatus status;

    private LocalDateTime startDateFrom;
    private LocalDateTime startDateTo;
    private LocalDateTime endDateFrom;
    private LocalDateTime endDateTo;

    private String periodName;
    private Long remainingDays; // Se filtrará como "igual o menor a X días restantes"
}
