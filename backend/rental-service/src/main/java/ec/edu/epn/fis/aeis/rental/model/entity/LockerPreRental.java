package ec.edu.epn.fis.aeis.rental.model.entity;

import ec.edu.epn.fis.aeis.rental.model.enums.PreRentalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class LockerPreRental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Long lockerId;

    @Column(nullable = false)
    private Integer lockerNumber;

    @Column(nullable = false)
    private String blockName;

    @Column(nullable = false)
    private Boolean allowCustomRental;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column
    private BigDecimal amountToPay;

    @Column(nullable = false, unique = true)
    private String payPhoneClientTransactionId;

    @Column
    private Long payPhoneTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreRentalStatus status = PreRentalStatus.PENDING;
}
