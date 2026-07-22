package ec.edu.epn.fis.aeis.rental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.dto.PreRentalRequestDTO;
import ec.edu.epn.fis.aeis.rental.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerPreRental;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.model.enums.PreRentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerPreRentalRepository;
import ec.edu.epn.fis.aeis.rental.repository.PeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el flujo de renta con PayPhone a través de llamadas HTTP reales
 * (ver PLAN.md §6.3), sin frontend ni Cajita de Pagos:
 *
 * (a) POST /rentals/pre-rentals deja el pre-alquiler PENDING y devuelve los
 *     datos completos para la Cajita.
 * (c) GET /payments/payphone/confirm con una transacción inexistente devuelve
 *     un error controlado (404, no un 500).
 *
 * locker-service/auth-service se simulan con @MockitoBean sobre los clientes
 * internos; PayPhone jamás se contacta de verdad en estos tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreRentalFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private LockerPreRentalRepository preRentalRepository;

    @MockitoBean
    private LockerServiceClient lockerServiceClient;

    @BeforeEach
    void setUp() {
        periodRepository.deleteAll();
        preRentalRepository.deleteAll();

        Period activePeriod = new Period();
        activePeriod.setName("2026-A");
        activePeriod.setStartDate(LocalDateTime.now().minusDays(5));
        activePeriod.setEndDate(LocalDateTime.now().plusMonths(4));
        activePeriod.setActive(true);
        periodRepository.save(activePeriod);
    }

    @Test
    void givenAvailableLocker_whenPostPreRental_thenPersistsPendingPreRentalAndReturnsCompleteCajitaData() throws Exception {
        ReserveResponseDTO reserved = new ReserveResponseDTO();
        reserved.setId(42L);
        reserved.setNumber(7);
        reserved.setBlockName("Bloque B");
        reserved.setAllowCustomRental(false);
        when(lockerServiceClient.reserve(42L)).thenReturn(reserved);

        PreRentalRequestDTO request = new PreRentalRequestDTO();
        request.setLockerId(42L);

        mockMvc.perform(post("/rentals/pre-rentals")
                        .header("X-User-Id", "0102030405")
                        .header("X-Username", "jdoe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preRentalId").isNumber())
                .andExpect(jsonPath("$.clientTransactionId").isNotEmpty())
                .andExpect(jsonPath("$.amountCents").value(650))
                .andExpect(jsonPath("$.reference").value("Alquiler de Casillero #7 del bloque Bloque B"))
                .andExpect(jsonPath("$.payphoneToken").exists())
                .andExpect(jsonPath("$.payphoneStoreId").exists());

        List<LockerPreRental> preRentals = preRentalRepository.findAll();
        assertThat(preRentals).hasSize(1);
        LockerPreRental preRental = preRentals.get(0);
        assertThat(preRental.getStatus()).isEqualTo(PreRentalStatus.PENDING);
        assertThat(preRental.getLockerId()).isEqualTo(42L);
        assertThat(preRental.getUsername()).isEqualTo("jdoe");
    }

    @Test
    void givenNonExistentClientTransactionId_whenConfirmPayment_thenReturnsControlledNotFoundError() throws Exception {
        assertThat(preRentalRepository.findByPayPhoneClientTransactionId("no-existe")).isEqualTo(Optional.empty());

        mockMvc.perform(get("/payments/payphone/confirm")
                        .param("id", "1")
                        .param("clientTransactionId", "no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontró un pre-alquiler para el ID proporcionado."));
    }
}
