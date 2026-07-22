package ec.edu.epn.fis.aeis.locker.dto;

import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReserveResponseDTO {

    private Long id;
    private Integer number;
    private String blockName;
    private Boolean allowCustomRental;

    public ReserveResponseDTO(Locker locker) {
        this.id = locker.getId();
        this.number = locker.getNumber();
        this.blockName = locker.getLockerBlock().getName();
        this.allowCustomRental = locker.getLockerBlock().getAllowCustomRental();
    }
}
