package ec.edu.epn.fis.aeis.rental.model.entity;

import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LockerRental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String username;

    @Column
    private String userFullName;

    @Column(nullable = false)
    private Long lockerId;

    @Column(nullable = false)
    private Integer lockerNumber;

    @Column(nullable = false)
    private String blockName;

    @OneToOne
    @JoinColumn(name = "locker_pre_rental_id", unique = true)
    private LockerPreRental lockerPreRental;

    @ManyToOne
    @JoinColumn(name = "period_id")
    private Period period;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @NotNull
    @Column(nullable = false)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;
}
