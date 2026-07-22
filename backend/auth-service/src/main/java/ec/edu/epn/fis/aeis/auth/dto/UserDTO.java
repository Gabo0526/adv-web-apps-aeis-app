package ec.edu.epn.fis.aeis.auth.dto;

import ec.edu.epn.fis.aeis.auth.model.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {

    private String id;
    private String username;
    private String name;
    private String lastName;
    private String uniqueCode;
    private String email;
    private String college;
    private String enabled;

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.lastName = user.getLastName();
        this.uniqueCode = user.getUniqueCode();
        this.email = user.getEmail();
        this.college = user.getCollege().getDescription();
        this.enabled = user.getEnabled() ? "Usuario habilitado en el sistema" : "Usuario NO habilitado en el sistema";
    }
}
