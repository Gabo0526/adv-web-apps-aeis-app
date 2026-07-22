package ec.edu.epn.fis.aeis.locker.controller;

import ec.edu.epn.fis.aeis.locker.dto.LockerBlockCreateRequestDTO;
import ec.edu.epn.fis.aeis.locker.dto.LockerBlockDTO;
import ec.edu.epn.fis.aeis.locker.model.entity.LockerBlock;
import ec.edu.epn.fis.aeis.locker.service.LockerBlockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locker-blocks")
public class LockerBlockController {

    private final LockerBlockService lockerBlockService;

    public LockerBlockController(LockerBlockService lockerBlockService) {
        this.lockerBlockService = lockerBlockService;
    }

    @GetMapping
    public List<LockerBlockDTO> getAllLockerBlocks() {
        return lockerBlockService.findAllDTO();
    }

    @PostMapping
    public ResponseEntity<LockerBlockDTO> createLockerBlock(@Valid @RequestBody LockerBlockCreateRequestDTO request) {
        LockerBlock lockerBlock = lockerBlockService.createWithLockers(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LockerBlockDTO(lockerBlock));
    }
}
