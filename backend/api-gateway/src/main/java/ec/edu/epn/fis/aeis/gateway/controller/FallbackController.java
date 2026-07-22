package ec.edu.epn.fis.aeis.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Fallback del Circuit Breaker de help-service (PLAN.md §3.4).
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/help")
    public ResponseEntity<Map<String, String>> helpFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "El módulo de ayuda no está disponible en este momento. Intenta más tarde."));
    }
}
