package ec.edu.epn.fis.aeis.locker.dto;

import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.model.enums.LockerStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LockerDTO {

    private Long id;
    private Integer number;
    private LockerStatus status;
    private Double length;
    private Double width;
    private Double height;
    private Long blockId;
    private String blockName;

    public LockerDTO(Locker locker) {
        this.id = locker.getId();
        this.number = locker.getNumber();
        this.status = locker.getStatus();
        this.length = locker.getLength();
        this.width = locker.getWidth();
        this.height = locker.getHeight();
        this.blockId = locker.getLockerBlock().getId();
        this.blockName = locker.getLockerBlock().getName();
    }
}
