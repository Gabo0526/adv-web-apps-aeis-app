package ec.edu.epn.fis.aeis.rental.controller;

import ec.edu.epn.fis.aeis.rental.dto.PeriodDTO;
import ec.edu.epn.fis.aeis.rental.dto.PeriodRequestDTO;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.service.PeriodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El gateway exige rol ADMIN para las escrituras de este prefijo (/api/periods/**);
 * este servicio no vuelve a verificar el rol (ver SecurityConfig).
 */
@RestController
@RequestMapping("/periods")
public class PeriodController {

    private final PeriodService periodService;

    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    @GetMapping
    public List<PeriodDTO> getAllPeriods() {
        return periodService.findAllDTO();
    }

    @GetMapping("/active")
    public ResponseEntity<PeriodDTO> getActivePeriod() {
        return periodService.getActivePeriod()
                .map(period -> ResponseEntity.ok(new PeriodDTO(period)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PeriodDTO> createPeriod(@Valid @RequestBody PeriodRequestDTO request) {
        Period period = periodService.saveNewPeriod(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PeriodDTO(period));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodDTO> updatePeriod(@PathVariable Long id, @Valid @RequestBody PeriodRequestDTO request) {
        Period period = periodService.updatePeriod(id, request);
        return ResponseEntity.ok(new PeriodDTO(period));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<PeriodDTO> activatePeriod(@PathVariable Long id) {
        Period period = periodService.activatePeriod(id);
        return ResponseEntity.ok(new PeriodDTO(period));
    }
}
