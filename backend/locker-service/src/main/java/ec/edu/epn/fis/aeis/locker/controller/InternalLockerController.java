package ec.edu.epn.fis.aeis.locker.controller;

import ec.edu.epn.fis.aeis.locker.dto.LockerDTO;
import ec.edu.epn.fis.aeis.locker.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.locker.model.entity.Locker;
import ec.edu.epn.fis.aeis.locker.service.LockerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo para comunicación entre servicios: el gateway nunca enruta /internal/**.
 * Lo consume rental-service para el flujo de renta (ver PLAN.md §5.3, §6.3).
 */
@RestController
@RequestMapping("/internal/lockers")
public class InternalLockerController {

    private final LockerService lockerService;

    public InternalLockerController(LockerService lockerService) {
        this.lockerService = lockerService;
    }

    @PutMapping("/{id}/reserve")
    public ReserveResponseDTO reserve(@PathVariable Long id) {
        Locker locker = lockerService.reserve(id);
        return new ReserveResponseDTO(locker);
    }

    @PutMapping("/{id}/occupy")
    public LockerDTO occupy(@PathVariable Long id) {
        Locker locker = lockerService.occupy(id);
        return new LockerDTO(locker);
    }

    @PutMapping("/{id}/release")
    public LockerDTO release(@PathVariable Long id) {
        Locker locker = lockerService.release(id);
        return new LockerDTO(locker);
    }
}
