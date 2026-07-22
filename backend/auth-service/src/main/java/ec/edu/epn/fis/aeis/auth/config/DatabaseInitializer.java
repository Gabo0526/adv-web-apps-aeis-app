package ec.edu.epn.fis.aeis.auth.config;

import ec.edu.epn.fis.aeis.auth.model.entity.Role;
import ec.edu.epn.fis.aeis.auth.model.entity.User;
import ec.edu.epn.fis.aeis.auth.model.enums.College;
import ec.edu.epn.fis.aeis.auth.repository.RoleRepository;
import ec.edu.epn.fis.aeis.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String ADMIN_ID = "9999999999";
    private static final String ADMIN_USERNAME = "admin";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminDefaultPassword;

    public DatabaseInitializer(RoleRepository roleRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.default-password}") String adminDefaultPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminDefaultPassword = adminDefaultPassword;
    }

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            Role userRole = roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(new Role("USER")));
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

            if (userRepository.existsById(ADMIN_ID) || userRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
                return;
            }

            if (adminDefaultPassword == null || adminDefaultPassword.isBlank()) {
                logger.warn("ADMIN_DEFAULT_PASSWORD no está configurada: se omite la creación del usuario admin");
                return;
            }

            User admin = new User();
            admin.setId(ADMIN_ID);
            admin.setUsername(ADMIN_USERNAME);
            admin.setName("Administrador");
            admin.setLastName("AEIS");
            admin.setUniqueCode("000000001");
            admin.setEmail("admin@aeis.epn.edu.ec");
            admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setCollege(College.FIS);
            admin.setRoles(Set.of(adminRole));
            admin.setEnabled(true);
            userRepository.save(admin);
            logger.info("Usuario ADMIN por defecto creado (username={})", ADMIN_USERNAME);
        };
    }
}
