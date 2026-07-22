package ec.edu.epn.fis.aeis.locker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LockerUpdateRequestDTO {

    @NotNull
    @Min(0)
    private Double length;

    @NotNull
    @Min(0)
    private Double width;

    @NotNull
    @Min(0)
    private Double height;
}
