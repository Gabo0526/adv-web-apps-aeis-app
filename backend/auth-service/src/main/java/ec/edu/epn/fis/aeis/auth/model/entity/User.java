package ec.edu.epn.fis.aeis.auth.model.entity;

import ec.edu.epn.fis.aeis.auth.model.enums.College;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_user")
public class User {

    @Id
    @NotBlank(message = "La cédula es obligatoria")
    @Size(min = 10, max = 10, message = "La cédula debe tener exactamente 10 dígitos")
    @Pattern(regexp = "\\d{10}", message = "La cédula debe contener solo números")
    @Column(length = 10)
    private String id;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 30, message = "El nombre de usuario debe tener entre 4 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "El nombre de usuario solo puede contener letras, números, guiones bajos y puntos")
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @NotBlank(message = "El código único es obligatorio")
    @Size(min = 9, max = 9, message = "El código único debe tener exactamente 9 dígitos")
    @Pattern(regexp = "\\d{9}", message = "El código único debe contener solo números")
    @Column(nullable = false, unique = true, length = 9)
    private String uniqueCode;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private College college;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<VerificationToken> verificationTokens;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @Column(nullable = false)
    private Boolean enabled = false;
}
