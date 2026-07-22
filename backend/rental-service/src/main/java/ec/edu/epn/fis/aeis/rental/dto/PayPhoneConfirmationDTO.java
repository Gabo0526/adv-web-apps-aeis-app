package ec.edu.epn.fis.aeis.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body enviado a la API V2/Confirm de PayPhone.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayPhoneConfirmationDTO {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("clientTxId")
    private String clientTxId;
}
