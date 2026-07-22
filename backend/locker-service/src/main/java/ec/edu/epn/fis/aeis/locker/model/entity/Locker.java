package ec.edu.epn.fis.aeis.locker.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.edu.epn.fis.aeis.locker.model.enums.LockerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"locker_block_id", "number"})
        }
)
public class Locker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "locker_block_id", nullable = false)
    @JsonIgnore
    private LockerBlock lockerBlock;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer number;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LockerStatus status = LockerStatus.AVAILABLE;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double length;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double width;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double height;
}
