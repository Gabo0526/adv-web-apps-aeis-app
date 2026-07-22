package ec.edu.epn.fis.aeis.auth.dto;

import ec.edu.epn.fis.aeis.auth.model.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalUserDTO {

    private String id;
    private String username;
    private String name;
    private String lastName;
    private String email;

    public InternalUserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
    }
}
