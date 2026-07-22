package ec.edu.epn.fis.aeis.rental.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mapea la respuesta de auth-service a GET /internal/users/{username}.
 */
@Getter
@Setter
@NoArgsConstructor
public class InternalUserDTO {
    private String id;
    private String username;
    private String name;
    private String lastName;
    private String email;
}
