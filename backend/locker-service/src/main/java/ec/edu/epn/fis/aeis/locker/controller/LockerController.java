package ec.edu.epn.fis.aeis.locker.controller;

import ec.edu.epn.fis.aeis.locker.dto.LockerDTO;
import ec.edu.epn.fis.aeis.locker.dto.LockerUpdateRequestDTO;
import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.service.LockerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lockers")
public class LockerController {

    private final LockerService lockerService;

    public LockerController(LockerService lockerService) {
        this.lockerService = lockerService;
    }

    @GetMapping("/block/{blockId}")
    public List<LockerDTO> getLockersByBlock(@PathVariable Long blockId) {
        return lockerService.findByLockerBlockId(blockId).stream().map(LockerDTO::new).toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<LockerDTO> updateLocker(@PathVariable Long id,
                                                    @Valid @RequestBody LockerUpdateRequestDTO request) {
        Locker locker = lockerService.updateDimensions(id, request);
        return ResponseEntity.ok(new LockerDTO(locker));
    }

    @PutMapping("/{id}/toggle-maintenance")
    public ResponseEntity<LockerDTO> toggleMaintenance(@PathVariable Long id) {
        Locker locker = lockerService.toggleMaintenanceStatus(id);
        return ResponseEntity.ok(new LockerDTO(locker));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocker(@PathVariable Long id) {
        lockerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
