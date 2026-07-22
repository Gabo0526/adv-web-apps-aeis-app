package ec.edu.epn.fis.aeis.auth.controller;

import ec.edu.epn.fis.aeis.auth.dto.ForgotPasswordRequestDTO;
import ec.edu.epn.fis.aeis.auth.dto.LoginRequestDTO;
import ec.edu.epn.fis.aeis.auth.dto.LoginResponseDTO;
import ec.edu.epn.fis.aeis.auth.dto.MessageResponseDTO;
import ec.edu.epn.fis.aeis.auth.dto.RegisterRequestDTO;
import ec.edu.epn.fis.aeis.auth.dto.ResetPasswordRequestDTO;
import ec.edu.epn.fis.aeis.auth.dto.VerifyResponseDTO;
import ec.edu.epn.fis.aeis.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthRestController {

    private final UserService userService;

    public AuthRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        userService.register(request.getId(), request.getUsername(), request.getName(), request.getLastName(),
                request.getUniqueCode(), request.getEmail(), request.getPassword(), request.getCollege());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponseDTO("Usuario registrado. Revisa tu correo para verificar la cuenta."));
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyResponseDTO> verify(@RequestParam String token) {
        boolean verified = userService.verifyAccount(token);
        String message = verified
                ? "¡Tu cuenta ha sido verificada exitosamente! Ya puedes iniciar sesión."
                : "No pudimos verificar tu cuenta. El token es inválido o ya ha sido utilizado.";
        return ResponseEntity.ok(new VerifyResponseDTO(verified, message));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new MessageResponseDTO(
                "Si el correo existe en nuestro sistema, recibirás un enlace para restablecer tu contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponseDTO("Contraseña actualizada correctamente."));
    }
}
