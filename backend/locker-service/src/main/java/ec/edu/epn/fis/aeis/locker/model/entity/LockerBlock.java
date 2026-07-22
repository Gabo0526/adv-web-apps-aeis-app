package ec.edu.epn.fis.aeis.locker.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Period vive en rental-service; aquí solo se guarda su id (sin FK ni entidad Period).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LockerBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Min(1)
    @Column(nullable = false)
    private Integer blockRows;

    @Min(1)
    @Column(nullable = false)
    private Integer blockColumns;

    @NotNull
    @Column(nullable = false)
    private Long periodId;

    @OneToMany(mappedBy = "lockerBlock", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<Locker> lockers;

    @Column(nullable = false)
    private Boolean allowCustomRental = false;
}
