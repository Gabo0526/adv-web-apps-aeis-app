package ec.edu.epn.fis.aeis.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VerifyResponseDTO {
    private boolean verified;
    private String message;
}
