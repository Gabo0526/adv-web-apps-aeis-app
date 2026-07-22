package ec.edu.epn.fis.aeis.rental.dto;

import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PeriodDTO {

    private Long id;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;

    public PeriodDTO(Period period) {
        this.id = period.getId();
        this.name = period.getName();
        this.startDate = period.getStartDate();
        this.endDate = period.getEndDate();
        this.active = period.getActive();
    }
}
