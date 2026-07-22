package ec.edu.epn.fis.aeis.auth.controller;

import ec.edu.epn.fis.aeis.auth.dto.PageResponseDTO;
import ec.edu.epn.fis.aeis.auth.dto.UserDTO;
import ec.edu.epn.fis.aeis.auth.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * El gateway exige rol ADMIN para este prefijo (/api/users/**); este servicio
 * no vuelve a verificar el rol (ver SecurityConfig).
 */
@RestController
@RequestMapping("/users")
public class AdminUserRestController {

    private final UserService userService;

    public AdminUserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<UserDTO>> getPaginatedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> pageResult = userService.getAllUsers(pageable);
        return ResponseEntity.ok(new PageResponseDTO<>(pageResult));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsersByIdPrefix(@RequestParam("idPrefix") String idPrefix) {
        if (idPrefix.length() < 3) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
        return ResponseEntity.ok(userService.searchUsersByIdPrefix(idPrefix));
    }
}
