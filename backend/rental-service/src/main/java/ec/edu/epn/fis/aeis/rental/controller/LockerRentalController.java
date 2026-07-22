package ec.edu.epn.fis.aeis.rental.controller;

import ec.edu.epn.fis.aeis.rental.dto.ExceptionalRentalRequestDTO;
import ec.edu.epn.fis.aeis.rental.dto.LockerRentalDTO;
import ec.edu.epn.fis.aeis.rental.dto.LockerRentalFilterDTO;
import ec.edu.epn.fis.aeis.rental.dto.PageResponseDTO;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import ec.edu.epn.fis.aeis.rental.service.LockerRentalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El gateway exige rol ADMIN para /api/rentals/admin/** (ver AccessRules del gateway);
 * este servicio no vuelve a verificar el rol (ver SecurityConfig).
 */
@RestController
@RequestMapping("/rentals")
public class LockerRentalController {

    private final LockerRentalService lockerRentalService;

    public LockerRentalController(LockerRentalService lockerRentalService) {
        this.lockerRentalService = lockerRentalService;
    }

    @GetMapping("/admin")
    public PageResponseDTO<LockerRentalDTO> getPaginatedLockerRentals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Pageable pageable = PageRequest.of(page, size, sort(sortBy, direction));
        Page<LockerRentalDTO> pageResult = lockerRentalService.getAllLockerRentals(pageable);
        return new PageResponseDTO<>(pageResult);
    }

    @GetMapping("/admin/filtered")
    public PageResponseDTO<LockerRentalDTO> getFilteredLockerRentals(
            @ModelAttribute LockerRentalFilterDTO filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Pageable pageable = PageRequest.of(page, size, sort(sortBy, direction));
        Page<LockerRentalDTO> pageResult = lockerRentalService.getFilteredLockerRentals(filters, pageable);
        return new PageResponseDTO<>(pageResult);
    }

    @GetMapping("/admin/statuses")
    public List<RentalStatus> getAllRentalStatuses() {
        return List.of(RentalStatus.values());
    }

    @PostMapping("/admin/exceptional")
    public ResponseEntity<LockerRentalDTO> createExceptionalRental(@Valid @RequestBody ExceptionalRentalRequestDTO request) {
        LockerRental rental = lockerRentalService.createExceptionalRental(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LockerRentalDTO(rental));
    }

    @GetMapping("/mine")
    public List<LockerRentalDTO> getMine(@RequestHeader("X-User-Id") String userId) {
        return lockerRentalService.getMine(userId);
    }

    private Sort sort(String sortBy, String direction) {
        return direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
    }
}
