package ec.edu.epn.fis.aeis.auth.service;

import ec.edu.epn.fis.aeis.auth.dto.LoginResponseDTO;
import ec.edu.epn.fis.aeis.auth.dto.UserDTO;
import ec.edu.epn.fis.aeis.auth.exception.AccountNotVerifiedException;
import ec.edu.epn.fis.aeis.auth.exception.DuplicateUserException;
import ec.edu.epn.fis.aeis.auth.exception.InvalidCredentialsException;
import ec.edu.epn.fis.aeis.auth.exception.InvalidTokenException;
import ec.edu.epn.fis.aeis.auth.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.auth.model.entity.PasswordResetToken;
import ec.edu.epn.fis.aeis.auth.model.entity.Role;
import ec.edu.epn.fis.aeis.auth.model.entity.User;
import ec.edu.epn.fis.aeis.auth.model.entity.VerificationToken;
import ec.edu.epn.fis.aeis.auth.model.enums.College;
import ec.edu.epn.fis.aeis.auth.repository.PasswordResetTokenRepository;
import ec.edu.epn.fis.aeis.auth.repository.RoleRepository;
import ec.edu.epn.fis.aeis.auth.repository.UserRepository;
import ec.edu.epn.fis.aeis.auth.repository.VerificationTokenRepository;
import ec.edu.epn.fis.aeis.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final String frontendUrl;

    public UserService(UserRepository userRepository,
                        VerificationTokenRepository verificationTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        RoleRepository roleRepository,
                        EmailService emailService,
                        PasswordEncoder encoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    public void register(String id, String username, String name, String lastName, String uniqueCode, String email, String password, College college) {
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("Ya existe un usuario con ese nombre de usuario o email");
        }
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Rol USER no existe"));

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(name);
        user.setLastName(lastName);
        user.setUniqueCode(uniqueCode);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setCollege(college);
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiration(LocalDateTime.now().plusDays(1));
        verificationTokenRepository.save(verificationToken);

        String url = frontendUrl + "/verify?token=" + token;
        String text = "Por favor, verifica tu cuenta haciendo clic en el siguiente enlace: " + url;
        emailService.sendEmail(email, "Verificación de cuenta", text);
    }

    public boolean verifyAccount(String token) {
        Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository.findByToken(token);
        if (verificationTokenOpt.isPresent()) {
            VerificationToken verificationToken = verificationTokenOpt.get();

            if (verificationToken.getConsumed()) {
                return false;
            }

            if (verificationToken.getExpiration().isBefore(LocalDateTime.now())) {
                return false;
            }

            User user = verificationToken.getUser();
            user.setEnabled(true);
            userRepository.save(user);

            verificationToken.setConsumed(true);
            verificationTokenRepository.save(verificationToken);

            return true;
        }
        return false;
    }

    public LoginResponseDTO login(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException ex) {
            throw new AccountNotVerifiedException("La cuenta no ha sido verificada. Revisa tu correo electrónico.");
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        User user = findByUsername(username);
        String token = jwtService.generateToken(user);
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();

        return new LoginResponseDTO(token, user.getUsername(), user.getName() + " " + user.getLastName(), roles);
    }

    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiration(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        String url = frontendUrl + "/reset-password?token=" + token;
        String text = "Para restablecer tu contraseña, haz clic en el siguiente enlace: " + url
                + "\nEste enlace expira en 1 hora.";
        emailService.sendEmail(email, "Restablecimiento de contraseña", text);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("El token es inválido o ya ha sido utilizado"));

        if (resetToken.getConsumed()) {
            throw new InvalidTokenException("El token es inválido o ya ha sido utilizado");
        }
        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("El token ha expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setConsumed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserDTO::new);
    }

    public List<UserDTO> searchUsersByIdPrefix(String idPrefix) {
        return userRepository.findByIdStartingWith(idPrefix)
                .stream()
                .map(UserDTO::new)
                .toList();
    }
}
