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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, verificationTokenRepository, passwordResetTokenRepository,
                roleRepository, emailService, encoder, authenticationManager, jwtService, "http://localhost:5173");
    }

    @Test
    void whenRegisterNewUser_thenSucceeds() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role()));
        when(encoder.encode(anyString())).thenReturn("encodedPassword");

        userService.register("123", "testuser", "Test", "User", "T001", "test@example.com", "password", College.FIS);

        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void whenRegisterWithExistingUsername_thenThrowsDuplicateUserException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User()));
        assertThrows(DuplicateUserException.class, () ->
                userService.register("123", "testuser", "Test", "User", "T001", "test@example.com", "password", College.FIS));
    }

    @Test
    void whenRegisterWithExistingEmail_thenThrowsDuplicateUserException() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));
        assertThrows(DuplicateUserException.class, () ->
                userService.register("123", "testuser", "Test", "User", "T001", "test@example.com", "password", College.FIS));
    }

    @Test
    void whenRegisterAndRoleNotFound_thenThrowsException() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () ->
                userService.register("123", "testuser", "Test", "User", "T001", "test@example.com", "password", College.FIS));
    }

    @Test
    void whenVerifyAccountWithValidToken_thenSucceeds() {
        VerificationToken token = new VerificationToken();
        token.setUser(new User());
        token.setExpiration(LocalDateTime.now().plusDays(1));
        token.setConsumed(false);
        when(verificationTokenRepository.findByToken("validToken")).thenReturn(Optional.of(token));

        assertTrue(userService.verifyAccount("validToken"));
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void whenVerifyAccountWithInvalidToken_thenReturnFalse() {
        when(verificationTokenRepository.findByToken("invalidToken")).thenReturn(Optional.empty());
        assertFalse(userService.verifyAccount("invalidToken"));
    }

    @Test
    void whenVerifyAccountWithConsumedToken_thenReturnFalse() {
        VerificationToken token = new VerificationToken();
        token.setConsumed(true);
        when(verificationTokenRepository.findByToken("consumedToken")).thenReturn(Optional.of(token));
        assertFalse(userService.verifyAccount("consumedToken"));
    }

    @Test
    void whenVerifyAccountWithExpiredToken_thenReturnFalse() {
        VerificationToken token = new VerificationToken();
        token.setExpiration(LocalDateTime.now().minusDays(1));
        token.setConsumed(false);
        when(verificationTokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(token));
        assertFalse(userService.verifyAccount("expiredToken"));
    }

    @Test
    void whenFindByUsername_thenReturnUser() {
        User user = new User();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        User found = userService.findByUsername("testuser");
        assertEquals("testuser", found.getUsername());
    }

    @Test
    void whenFindByUsernameNotFound_thenThrowsException() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.findByUsername("unknown"));
    }

    @Test
    void whenGetAllUsers_thenReturnPageOfUserDTO() {
        User user = new User();
        user.setCollege(College.FIS);
        user.setRoles(Set.of());
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        Page<UserDTO> result = userService.getAllUsers(Pageable.unpaged());
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void whenSearchUsersByIdPrefix_thenReturnListOfUserDTO() {
        User user = new User();
        user.setId("12345");
        user.setCollege(College.FIS);
        user.setRoles(Set.of());
        when(userRepository.findByIdStartingWith("123")).thenReturn(Collections.singletonList(user));

        List<UserDTO> result = userService.searchUsersByIdPrefix("123");
        assertFalse(result.isEmpty());
        assertEquals("12345", result.get(0).getId());
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnsToken() {
        User user = new User();
        user.setUsername("testuser");
        user.setName("Test");
        user.setLastName("User");
        user.setRoles(Set.of(new Role("USER")));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponseDTO response = userService.login("testuser", "password");

        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertTrue(response.getRoles().contains("USER"));
    }

    @Test
    void whenLoginWithBadCredentials_thenThrowsInvalidCredentialsException() {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());
        assertThrows(InvalidCredentialsException.class, () -> userService.login("testuser", "wrongpassword"));
    }

    @Test
    void whenLoginWithDisabledAccount_thenThrowsAccountNotVerifiedException() {
        doThrow(new DisabledException("disabled")).when(authenticationManager).authenticate(any());
        assertThrows(AccountNotVerifiedException.class, () -> userService.login("testuser", "password"));
    }

    @Test
    void whenForgotPasswordWithExistingEmail_thenCreatesTokenAndSendsEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        userService.forgotPassword("test@example.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void whenForgotPasswordWithUnknownEmail_thenDoesNothingSilently() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        userService.forgotPassword("unknown@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void whenResetPasswordWithValidToken_thenUpdatesPassword() {
        User user = new User();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setConsumed(false);
        token.setExpiration(LocalDateTime.now().plusHours(1));
        when(passwordResetTokenRepository.findByToken("validToken")).thenReturn(Optional.of(token));
        when(encoder.encode("newPassword")).thenReturn("encodedNewPassword");

        userService.resetPassword("validToken", "newPassword");

        assertEquals("encodedNewPassword", user.getPassword());
        assertTrue(token.getConsumed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void whenResetPasswordWithInvalidToken_thenThrowsInvalidTokenException() {
        when(passwordResetTokenRepository.findByToken("invalidToken")).thenReturn(Optional.empty());
        assertThrows(InvalidTokenException.class, () -> userService.resetPassword("invalidToken", "newPassword"));
    }

    @Test
    void whenResetPasswordWithConsumedToken_thenThrowsInvalidTokenException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setConsumed(true);
        when(passwordResetTokenRepository.findByToken("consumedToken")).thenReturn(Optional.of(token));
        assertThrows(InvalidTokenException.class, () -> userService.resetPassword("consumedToken", "newPassword"));
    }

    @Test
    void whenResetPasswordWithExpiredToken_thenThrowsInvalidTokenException() {
        PasswordResetToken token = new PasswordResetToken();
        token.setConsumed(false);
        token.setExpiration(LocalDateTime.now().minusHours(1));
        when(passwordResetTokenRepository.findByToken("expiredToken")).thenReturn(Optional.of(token));
        assertThrows(InvalidTokenException.class, () -> userService.resetPassword("expiredToken", "newPassword"));
    }
}
