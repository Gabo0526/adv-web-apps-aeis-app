package ec.edu.epn.fis.aeis.rental.controller;

import ec.edu.epn.fis.aeis.rental.dto.PreRentalRequestDTO;
import ec.edu.epn.fis.aeis.rental.dto.PreRentalResponseDTO;
import ec.edu.epn.fis.aeis.rental.service.LockerPreRentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicia el flujo de renta con PayPhone (ver PLAN.md §6.3). El gateway exige
 * autenticación para /api/rentals/**; este servicio lee la identidad de los
 * headers X-User-Id/X-Username que el gateway agrega tras validar el JWT.
 */
@RestController
@RequestMapping("/rentals/pre-rentals")
public class PreRentalController {

    private final LockerPreRentalService lockerPreRentalService;

    public PreRentalController(LockerPreRentalService lockerPreRentalService) {
        this.lockerPreRentalService = lockerPreRentalService;
    }

    @PostMapping
    public ResponseEntity<PreRentalResponseDTO> createPreRental(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username,
            @Valid @RequestBody PreRentalRequestDTO request) {
        PreRentalResponseDTO response = lockerPreRentalService.createPreRental(
                userId, username, request.getLockerId(), request.getStartDate(), request.getEndDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
