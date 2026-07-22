package ec.edu.epn.fis.aeis.auth.service;

import ec.edu.epn.fis.aeis.auth.model.entity.Role;
import ec.edu.epn.fis.aeis.auth.model.entity.User;
import ec.edu.epn.fis.aeis.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setName("ADMIN");

        userRole = new Role();
        userRole.setName("USER");

        testUser = new User();
        testUser.setUsername("jdoe");
        testUser.setPassword("encryptedpass");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(adminRole));
    }

    @Test
    void givenExistingUsername_whenLoadUserByUsername_thenReturnsUserDetails() {
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jdoe");

        assertNotNull(userDetails);
        assertEquals("jdoe", userDetails.getUsername());
        assertEquals("encryptedpass", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        assertEquals(1, userDetails.getAuthorities().size());

        verify(userRepository).findByUsername("jdoe");
    }

    @Test
    void givenUserWithMultipleRoles_whenLoadUserByUsername_thenReturnsAllAuthorities() {
        testUser.setRoles(Set.of(adminRole, userRole));
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jdoe");

        assertNotNull(userDetails);
        assertEquals(2, userDetails.getAuthorities().size());

        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));

        verify(userRepository).findByUsername("jdoe");
    }

    @Test
    void givenDisabledUser_whenLoadUserByUsername_thenReturnsDisabledUserDetails() {
        testUser.setEnabled(false);
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jdoe");

        assertNotNull(userDetails);
        assertEquals("jdoe", userDetails.getUsername());
        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));

        verify(userRepository).findByUsername("jdoe");
    }

    @Test
    void givenUserWithNoRoles_whenLoadUserByUsername_thenReturnsUserWithEmptyAuthorities() {
        testUser.setRoles(Set.of());
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jdoe");

        assertNotNull(userDetails);
        assertEquals("jdoe", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().isEmpty());

        verify(userRepository).findByUsername("jdoe");
    }

    @Test
    void givenNonExistingUsername_whenLoadUserByUsername_thenThrowsException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void givenNullUsername_whenLoadUserByUsername_thenThrowsException() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(null)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findByUsername(null);
    }

    @Test
    void givenEmptyUsername_whenLoadUserByUsername_thenThrowsException() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("")
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).findByUsername("");
    }

    @Test
    void givenUserWithSpecialCharactersInRole_whenLoadUserByUsername_thenReturnsCorrectAuthority() {
        Role specialRole = new Role();
        specialRole.setName("SUPER_ADMIN");
        testUser.setRoles(Set.of(specialRole));
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jdoe");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN")));

        verify(userRepository).findByUsername("jdoe");
    }
}
