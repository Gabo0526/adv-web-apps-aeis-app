package ec.edu.epn.fis.aeis.help.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketRequestDTO {

    @NotBlank
    private String subject;

    @NotBlank
    private String description;

    private String rentalRef;
}
