package ec.edu.epn.fis.aeis.auth.dto;

import ec.edu.epn.fis.aeis.auth.model.enums.College;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {

    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "\\d{10}", message = "La cédula debe tener exactamente 10 dígitos")
    private String id;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 30, message = "El nombre de usuario debe tener entre 4 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "El nombre de usuario solo puede contener letras, números, guiones bajos y puntos")
    private String username;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @NotBlank(message = "El código único es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El código único debe tener exactamente 9 dígitos")
    private String uniqueCode;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotNull(message = "La facultad es obligatoria")
    private College college;
}
