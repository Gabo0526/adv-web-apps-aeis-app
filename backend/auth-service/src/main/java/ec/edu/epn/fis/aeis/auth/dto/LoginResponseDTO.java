package ec.edu.epn.fis.aeis.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String username;
    private String name;
    private List<String> roles;
}
