package ec.edu.epn.fis.aeis.help.controller;

import ec.edu.epn.fis.aeis.help.dto.CreateTicketRequestDTO;
import ec.edu.epn.fis.aeis.help.dto.MessageDTO;
import ec.edu.epn.fis.aeis.help.dto.TicketDTO;
import ec.edu.epn.fis.aeis.help.exception.ForbiddenException;
import ec.edu.epn.fis.aeis.help.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * El gateway solo exige un JWT válido para /api/help/**; los roles ADMIN de
 * "todos los tickets" y "cerrar ticket", y el control dueño-o-admin de los
 * mensajes, se verifican aquí (ver PLAN.md §7.2 y AccessRules del gateway).
 */
@RestController
@RequestMapping("/help")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/tickets")
    public ResponseEntity<TicketDTO> createTicket(@RequestHeader("X-User-Id") String userId,
                                                   @RequestHeader("X-Username") String username,
                                                   @Valid @RequestBody CreateTicketRequestDTO request) {
        TicketDTO ticket = ticketService.createTicket(userId, username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/tickets/mine")
    public List<TicketDTO> getMine(@RequestHeader("X-User-Id") String userId) {
        return ticketService.getMine(userId);
    }

    @GetMapping("/tickets")
    public List<TicketDTO> getAll(@RequestHeader("X-User-Roles") String rolesHeader) {
        requireAdmin(rolesHeader);
        return ticketService.getAll();
    }

    @GetMapping("/tickets/{id}/messages")
    public List<MessageDTO> getMessages(@PathVariable String id,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Roles") String rolesHeader) {
        return ticketService.getMessages(id, userId, splitRoles(rolesHeader));
    }

    @PutMapping("/tickets/{id}/close")
    public TicketDTO closeTicket(@PathVariable String id,
                                  @RequestHeader("X-Username") String username,
                                  @RequestHeader("X-User-Roles") String rolesHeader) {
        requireAdmin(rolesHeader);
        return ticketService.closeTicket(id, username);
    }

    private void requireAdmin(String rolesHeader) {
        if (!splitRoles(rolesHeader).contains("ADMIN")) {
            throw new ForbiddenException("Requiere rol ADMIN");
        }
    }

    private List<String> splitRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }
        return Arrays.asList(rolesHeader.split(","));
    }
}
