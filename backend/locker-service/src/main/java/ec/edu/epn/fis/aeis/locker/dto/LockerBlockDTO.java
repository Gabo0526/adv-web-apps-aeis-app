package ec.edu.epn.fis.aeis.locker.dto;

import ec.edu.epn.fis.aeis.locker.model.entity.LockerBlock;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LockerBlockDTO {

    private Long id;
    private String name;
    private Integer blockRows;
    private Integer blockColumns;
    private Long periodId;
    private Boolean allowCustomRental;
    private List<LockerDTO> lockers;

    public LockerBlockDTO(LockerBlock block) {
        this.id = block.getId();
        this.name = block.getName();
        this.blockRows = block.getBlockRows();
        this.blockColumns = block.getBlockColumns();
        this.periodId = block.getPeriodId();
        this.allowCustomRental = block.getAllowCustomRental();
        this.lockers = block.getLockers() == null
                ? List.of()
                : block.getLockers().stream().map(LockerDTO::new).toList();
    }
}
