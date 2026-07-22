package ec.edu.epn.fis.aeis.locker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LockerBlockCreateRequestDTO {

    @NotBlank
    private String name;

    @NotNull
    @Min(1)
    private Integer blockRows;

    @NotNull
    @Min(1)
    private Integer blockColumns;

    @NotNull
    private Long periodId;

    private Boolean allowCustomRental = false;

    @NotNull
    @Min(0)
    private Double lockerLength;

    @NotNull
    @Min(0)
    private Double lockerWidth;

    @NotNull
    @Min(0)
    private Double lockerHeight;
}
