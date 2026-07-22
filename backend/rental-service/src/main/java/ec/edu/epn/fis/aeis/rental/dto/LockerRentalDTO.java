package ec.edu.epn.fis.aeis.rental.dto;

import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class LockerRentalDTO {

    private Long id;
    private String blockName;
    private Integer lockerNumber;

    private String userId;
    private String username;
    private String userFullName;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long remainingDays;

    private String periodName;
    private BigDecimal amountPaid;
    private RentalStatus status;

    public LockerRentalDTO(LockerRental rental) {
        this.id = rental.getId();
        this.blockName = rental.getBlockName();
        this.lockerNumber = rental.getLockerNumber();

        this.userId = rental.getUserId();
        this.username = rental.getUsername();
        this.userFullName = rental.getUserFullName();

        this.startDate = rental.getStartDate();
        this.endDate = rental.getEndDate();

        if (rental.getEndDate() != null) {
            long diff = ChronoUnit.DAYS.between(LocalDateTime.now(), rental.getEndDate());
            this.remainingDays = Math.max(diff, 0);
        } else {
            this.remainingDays = -1;
        }

        this.periodName = rental.getPeriod() != null ? rental.getPeriod().getName() : null;
        this.amountPaid = rental.getAmountPaid();
        this.status = rental.getStatus();
    }
}
