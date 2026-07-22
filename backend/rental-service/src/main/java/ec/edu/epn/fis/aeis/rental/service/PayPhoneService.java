package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.dto.PayPhoneAPIResponseDTO;
import ec.edu.epn.fis.aeis.rental.dto.PayPhoneConfirmationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class PayPhoneService {

    @Value("${payphone.token}")
    private String payPhoneToken;

    private static final String PAYPHONE_API_URL = "https://pay.payphonetodoesposible.com/api/button/V2/Confirm";
    private static final Logger log = LoggerFactory.getLogger(PayPhoneService.class);

    public PayPhoneAPIResponseDTO confirmPayPhonePayment(PayPhoneConfirmationDTO dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(payPhoneToken);

        HttpEntity<PayPhoneConfirmationDTO> request = new HttpEntity<>(dto, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<PayPhoneAPIResponseDTO> response = restTemplate
                    .postForEntity(PAYPHONE_API_URL, request, PayPhoneAPIResponseDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Confirmación exitosa desde PayPhone: {}", response.getBody().getReference());
                return response.getBody();
            }

            log.warn("Respuesta inesperada de PayPhone: {}", response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            log.error("Error HTTP al confirmar con PayPhone: {}", ex.getStatusCode());
            log.error("Respuesta: {}", ex.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error inesperado al confirmar con PayPhone", e);
        }

        return null;
    }
}
