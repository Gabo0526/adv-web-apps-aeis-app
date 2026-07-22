package ec.edu.epn.fis.aeis.rental.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapea la respuesta de la API V2/Confirm de PayPhone (ver PLAN.md §6.3).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayPhoneAPIResponseDTO {

    private Long amount;
    private String clientTransactionId;
    private Integer statusCode;
    @JsonProperty("transactionStatus")
    private String transactionStatus;
    private Long transactionId;

    private String email;
    private String cardType;
    private String bin;
    private String lastDigits;
    private String deferredCode;
    private String deferredMessage;
    private Boolean deferred;
    private String cardBrandCode;
    private String cardBrand;
    private String phoneNumber;
    private String authorizationCode;
    private String message;
    private Integer messageCode;
    private String document;
    private List<Object> taxes;
    private String currency;
    private String optionalParameter1;
    private String optionalParameter2;
    private String optionalParameter3;
    private String optionalParameter4;
    private String storeName;
    private LocalDateTime date;
    private String regionIso;
    private String transactionType;
    private Object recap;
    private String reference;
    private String pan;

    public Boolean isPaid() {
        return "Approved".equalsIgnoreCase(transactionStatus) || Integer.valueOf(3).equals(statusCode);
    }
}
