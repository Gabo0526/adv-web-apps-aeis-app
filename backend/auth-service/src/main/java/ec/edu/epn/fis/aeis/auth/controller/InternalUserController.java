package ec.edu.epn.fis.aeis.auth.controller;

import ec.edu.epn.fis.aeis.auth.dto.InternalUserDTO;
import ec.edu.epn.fis.aeis.auth.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo para comunicación entre servicios: el gateway nunca enruta /internal/**.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public InternalUserDTO getByUsername(@PathVariable String username) {
        return new InternalUserDTO(userService.findByUsername(username));
    }
}
