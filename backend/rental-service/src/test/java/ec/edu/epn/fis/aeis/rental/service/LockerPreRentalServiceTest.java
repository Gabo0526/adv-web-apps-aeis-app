package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.client.AuthServiceClient;
import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.dto.InternalUserDTO;
import ec.edu.epn.fis.aeis.rental.dto.PayPhoneAPIResponseDTO;
import ec.edu.epn.fis.aeis.rental.dto.PreRentalResponseDTO;
import ec.edu.epn.fis.aeis.rental.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.rental.exception.LockerConflictException;
import ec.edu.epn.fis.aeis.rental.exception.NoActivePeriodException;
import ec.edu.epn.fis.aeis.rental.exception.PayPhoneConfirmationException;
import ec.edu.epn.fis.aeis.rental.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerPreRental;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.model.enums.PreRentalStatus;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerPreRentalRepository;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre el flujo de renta con PayPhone (PLAN.md §6.3). La confirmación se
 * prueba mockeando PayPhoneAPIResponseDTO (pagado/rechazado) y PayPhoneService;
 * no se hace ninguna llamada real a la API de PayPhone en estos tests.
 */
@ExtendWith(MockitoExtension.class)
class LockerPreRentalServiceTest {

    @Mock
    private LockerPreRentalRepository preRentalRepository;
    @Mock
    private LockerRentalRepository lockerRentalRepository;
    @Mock
    private PeriodService periodService;
    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private LockerServiceClient lockerServiceClient;
    @Mock
    private PayPhoneService payPhoneService;

    private final RentalValidator rentalValidator = new RentalValidator();
    private final RentalCalculator rentalCalculator = new RentalCalculator();

    private LockerPreRentalService lockerPreRentalService;

    private Period activePeriod;
    private ReserveResponseDTO reserved;

    @BeforeEach
    void setUp() {
        lockerPreRentalService = new LockerPreRentalService(
                preRentalRepository, lockerRentalRepository, periodService,
                authServiceClient, lockerServiceClient, rentalValidator, rentalCalculator, payPhoneService);
        ReflectionTestUtils.setField(lockerPreRentalService, "payPhoneToken", "test-token");
        ReflectionTestUtils.setField(lockerPreRentalService, "payPhoneStoreId", "test-store-id");

        activePeriod = new Period();
        activePeriod.setId(1L);
        activePeriod.setName("2026-A");
        activePeriod.setStartDate(LocalDateTime.now().minusDays(5));
        activePeriod.setEndDate(LocalDateTime.now().plusMonths(4));
        activePeriod.setActive(true);

        reserved = new ReserveResponseDTO();
        reserved.setId(10L);
        reserved.setNumber(5);
        reserved.setBlockName("Bloque A");
        reserved.setAllowCustomRental(false);
    }

    // --- createPreRental ---

    @Test
    void givenPeriodRentalWithoutActivePeriod_whenCreatePreRental_thenThrowsAndCompensates() {
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);
        when(periodService.getActivePeriod()).thenReturn(Optional.empty());

        assertThrows(NoActivePeriodException.class,
                () -> lockerPreRentalService.createPreRental("0102030405", "jdoe", 10L, null, null));

        verify(lockerServiceClient).release(10L);
        verify(preRentalRepository, never()).save(any());
    }

    @Test
    void givenLockerNotAvailable_whenCreatePreRental_thenPropagatesConflictWithoutCompensating() {
        when(lockerServiceClient.reserve(10L))
                .thenThrow(new LockerConflictException("El casillero no está disponible para reservar"));

        assertThrows(LockerConflictException.class,
                () -> lockerPreRentalService.createPreRental("0102030405", "jdoe", 10L, null, null));

        verify(lockerServiceClient, never()).release(anyLong());
    }

    @Test
    void givenCustomEndDateOnLockerThatDoesNotAllowIt_whenCreatePreRental_thenCompensatesWithRelease() {
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);
        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));

        LocalDateTime endDate = LocalDateTime.now().plusDays(3);

        assertThrows(IllegalStateException.class,
                () -> lockerPreRentalService.createPreRental("0102030405", "jdoe", 10L, null, endDate));

        verify(lockerServiceClient).release(10L);
        verify(preRentalRepository, never()).save(any());
    }

    @Test
    void givenPeriodRental_whenCreatePreRental_thenLeavesLockerPendingAndReturnsCompleteCajitaData() {
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);
        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));
        when(preRentalRepository.save(any(LockerPreRental.class))).thenAnswer(inv -> {
            LockerPreRental p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        PreRentalResponseDTO response = lockerPreRentalService.createPreRental("0102030405", "jdoe", 10L, null, null);

        // El casillero queda PENDING porque locker-service ya lo transicionó en el reserve();
        // aquí verificamos que rental-service lo invocó y persistió el pre-alquiler como PENDING.
        verify(lockerServiceClient).reserve(10L);
        verify(lockerServiceClient, never()).release(anyLong());

        ArgumentCaptor<LockerPreRental> captor = ArgumentCaptor.forClass(LockerPreRental.class);
        verify(preRentalRepository).save(captor.capture());
        LockerPreRental saved = captor.getValue();
        assertEquals(PreRentalStatus.PENDING, saved.getStatus());
        assertEquals(10L, saved.getLockerId());
        assertEquals(activePeriod.getEndDate(), saved.getEndDate());
        assertEquals(BigDecimal.valueOf(6.5), saved.getAmountToPay());

        assertEquals(99L, response.getPreRentalId());
        assertEquals(saved.getPayPhoneClientTransactionId(), response.getClientTransactionId());
        assertEquals(650L, response.getAmountCents());
        assertEquals("Alquiler de Casillero #5 del bloque Bloque A", response.getReference());
        assertEquals("test-token", response.getPayphoneToken());
        assertEquals("test-store-id", response.getPayphoneStoreId());
    }

    @Test
    void givenCustomRentalWithinLimits_whenCreatePreRental_thenCalculatesDailyAmount() {
        reserved.setAllowCustomRental(true);
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);
        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));
        when(preRentalRepository.save(any(LockerPreRental.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(2);

        PreRentalResponseDTO response = lockerPreRentalService.createPreRental("0102030405", "jdoe", 10L, start, end);

        // 3 días (incluye el día de inicio) * 1.00 = 3.00 -> 300 centavos
        assertEquals(300L, response.getAmountCents());
    }

    // --- confirmPreRental ---

    @Test
    void givenNonExistentClientTransactionId_whenConfirmPreRental_thenThrowsResourceNotFound() {
        when(preRentalRepository.findByPayPhoneClientTransactionId("does-not-exist"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> lockerPreRentalService.confirmPreRental(1L, "does-not-exist"));

        verify(payPhoneService, never()).confirmPayPhonePayment(any());
    }

    @Test
    void givenAlreadyCompletedPreRental_whenConfirmPreRental_thenThrowsIllegalState() {
        LockerPreRental preRental = pendingPreRental();
        preRental.setStatus(PreRentalStatus.COMPLETED);
        when(preRentalRepository.findByPayPhoneClientTransactionId("client-123"))
                .thenReturn(Optional.of(preRental));

        assertThrows(IllegalStateException.class,
                () -> lockerPreRentalService.confirmPreRental(1L, "client-123"));

        verify(payPhoneService, never()).confirmPayPhonePayment(any());
    }

    @Test
    void givenPayPhoneApiFailure_whenConfirmPreRental_thenThrowsPayPhoneConfirmationException() {
        LockerPreRental preRental = pendingPreRental();
        when(preRentalRepository.findByPayPhoneClientTransactionId("client-123"))
                .thenReturn(Optional.of(preRental));
        when(payPhoneService.confirmPayPhonePayment(any())).thenReturn(null);

        assertThrows(PayPhoneConfirmationException.class,
                () -> lockerPreRentalService.confirmPreRental(1L, "client-123"));

        verify(lockerServiceClient, never()).occupy(anyLong());
        verify(lockerRentalRepository, never()).save(any());
    }

    @Test
    void givenPaidPayPhoneResponse_whenConfirmPreRental_thenOccupiesLockerAndActivatesRental() {
        LockerPreRental preRental = pendingPreRental();
        when(preRentalRepository.findByPayPhoneClientTransactionId("client-123"))
                .thenReturn(Optional.of(preRental));

        PayPhoneAPIResponseDTO paidResponse = new PayPhoneAPIResponseDTO();
        paidResponse.setTransactionStatus("Approved");
        paidResponse.setTransactionId(555L);
        when(payPhoneService.confirmPayPhonePayment(any())).thenReturn(paidResponse);

        InternalUserDTO user = new InternalUserDTO();
        user.setUsername("jdoe");
        user.setName("John");
        user.setLastName("Doe");
        when(authServiceClient.findByUsername("jdoe")).thenReturn(user);

        boolean result = lockerPreRentalService.confirmPreRental(1L, "client-123");

        assertTrue(result);
        verify(lockerServiceClient).occupy(10L);
        verify(lockerServiceClient, never()).release(anyLong());

        ArgumentCaptor<LockerRental> rentalCaptor = ArgumentCaptor.forClass(LockerRental.class);
        verify(lockerRentalRepository).save(rentalCaptor.capture());
        LockerRental rental = rentalCaptor.getValue();
        assertEquals(RentalStatus.ACTIVE, rental.getStatus());
        assertEquals("John Doe", rental.getUserFullName());
        assertEquals(10L, rental.getLockerId());

        assertEquals(PreRentalStatus.COMPLETED, preRental.getStatus());
        assertEquals(555L, preRental.getPayPhoneTransactionId());
    }

    @Test
    void givenRejectedPayPhoneResponse_whenConfirmPreRental_thenReleasesLockerAndCancelsPreRental() {
        LockerPreRental preRental = pendingPreRental();
        when(preRentalRepository.findByPayPhoneClientTransactionId("client-123"))
                .thenReturn(Optional.of(preRental));

        PayPhoneAPIResponseDTO rejectedResponse = new PayPhoneAPIResponseDTO();
        rejectedResponse.setTransactionStatus("Rejected");
        rejectedResponse.setStatusCode(0);
        rejectedResponse.setTransactionId(556L);
        when(payPhoneService.confirmPayPhonePayment(any())).thenReturn(rejectedResponse);

        boolean result = lockerPreRentalService.confirmPreRental(1L, "client-123");

        assertFalse(result);
        verify(lockerServiceClient).release(10L);
        verify(lockerServiceClient, never()).occupy(anyLong());
        verify(lockerRentalRepository, never()).save(any());
        assertEquals(PreRentalStatus.CANCELLED, preRental.getStatus());
    }

    @Test
    void givenOccupyAlwaysFailsAfterPaymentConfirmed_whenConfirmPreRental_thenRetriesThreeTimesAndStillCompletes() {
        LockerPreRental preRental = pendingPreRental();
        when(preRentalRepository.findByPayPhoneClientTransactionId("client-123"))
                .thenReturn(Optional.of(preRental));

        PayPhoneAPIResponseDTO paidResponse = new PayPhoneAPIResponseDTO();
        paidResponse.setTransactionStatus("Approved");
        paidResponse.setTransactionId(555L);
        when(payPhoneService.confirmPayPhonePayment(any())).thenReturn(paidResponse);
        when(authServiceClient.findByUsername("jdoe")).thenReturn(new InternalUserDTO());

        org.mockito.Mockito.doThrow(new RuntimeException("locker-service caído"))
                .when(lockerServiceClient).occupy(10L);

        boolean result = lockerPreRentalService.confirmPreRental(1L, "client-123");

        // Compensación simple sin Saga: el pago ya fue confirmado, así que se completa
        // el alquiler aunque occupy haya fallado tras 3 intentos (queda logueado a nivel ERROR).
        assertTrue(result);
        verify(lockerServiceClient, times(3)).occupy(10L);
        verify(lockerRentalRepository).save(any(LockerRental.class));
        assertEquals(PreRentalStatus.COMPLETED, preRental.getStatus());
    }

    private LockerPreRental pendingPreRental() {
        LockerPreRental preRental = new LockerPreRental();
        preRental.setId(1L);
        preRental.setUserId("0102030405");
        preRental.setUsername("jdoe");
        preRental.setLockerId(10L);
        preRental.setLockerNumber(5);
        preRental.setBlockName("Bloque A");
        preRental.setAllowCustomRental(false);
        preRental.setCreatedAt(LocalDateTime.now());
        preRental.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        preRental.setStartDate(LocalDateTime.now());
        preRental.setEndDate(activePeriod.getEndDate());
        preRental.setAmountToPay(BigDecimal.valueOf(6.5));
        preRental.setPayPhoneClientTransactionId("client-123");
        preRental.setStatus(PreRentalStatus.PENDING);
        return preRental;
    }
}
