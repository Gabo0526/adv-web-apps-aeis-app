package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.client.AuthServiceClient;
import ec.edu.epn.fis.aeis.rental.client.LockerServiceClient;
import ec.edu.epn.fis.aeis.rental.dto.ExceptionalRentalRequestDTO;
import ec.edu.epn.fis.aeis.rental.dto.InternalUserDTO;
import ec.edu.epn.fis.aeis.rental.dto.ReserveResponseDTO;
import ec.edu.epn.fis.aeis.rental.exception.LockerConflictException;
import ec.edu.epn.fis.aeis.rental.exception.NoActivePeriodException;
import ec.edu.epn.fis.aeis.rental.exception.ServiceUnavailableException;
import ec.edu.epn.fis.aeis.rental.model.entity.LockerRental;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.model.enums.RentalStatus;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockerRentalServiceTest {

    @Mock
    private LockerRentalRepository lockerRentalRepository;
    @Mock
    private PeriodService periodService;
    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private LockerServiceClient lockerServiceClient;

    private final RentalValidator rentalValidator = new RentalValidator();

    @InjectMocks
    private LockerRentalService lockerRentalService;

    private Period activePeriod;
    private ExceptionalRentalRequestDTO request;

    @BeforeEach
    void setUp() {
        // @InjectMocks no reemplaza campos ya asignados; se inyecta manualmente
        // el validador real (no un mock) para probar el flujo end-to-end.
        lockerRentalService = new LockerRentalService(
                lockerRentalRepository, periodService, authServiceClient, lockerServiceClient, rentalValidator);

        activePeriod = new Period();
        activePeriod.setId(1L);
        activePeriod.setName("2026-A");
        activePeriod.setStartDate(LocalDateTime.now().minusDays(5));
        activePeriod.setEndDate(LocalDateTime.now().plusMonths(4));
        activePeriod.setActive(true);

        request = new ExceptionalRentalRequestDTO();
        request.setUsername("jdoe");
        request.setLockerId(10L);
        request.setAmountPaid(BigDecimal.valueOf(6.5));
    }

    @Test
    void givenAvailableLockerAndNoActivePeriod_whenCreateExceptionalRental_thenThrowsNoActivePeriod() {
        when(periodService.getActivePeriod()).thenReturn(Optional.empty());

        assertThrows(NoActivePeriodException.class, () -> lockerRentalService.createExceptionalRental(request));
        verify(lockerServiceClient, never()).reserve(any());
    }

    @Test
    void givenValidRequest_whenCreateExceptionalRental_thenReservesOccupiesAndSaves() {
        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));

        InternalUserDTO user = new InternalUserDTO();
        user.setId("0102030405");
        user.setUsername("jdoe");
        user.setName("John");
        user.setLastName("Doe");
        when(authServiceClient.findByUsername("jdoe")).thenReturn(user);

        ReserveResponseDTO reserved = new ReserveResponseDTO();
        reserved.setId(10L);
        reserved.setNumber(5);
        reserved.setBlockName("Bloque A");
        reserved.setAllowCustomRental(false);
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);

        when(lockerRentalRepository.save(any(LockerRental.class))).thenAnswer(inv -> inv.getArgument(0));

        LockerRental result = lockerRentalService.createExceptionalRental(request);

        assertEquals("jdoe", result.getUsername());
        assertEquals("John Doe", result.getUserFullName());
        assertEquals(10L, result.getLockerId());
        assertEquals(RentalStatus.ACTIVE, result.getStatus());
        assertEquals(activePeriod.getEndDate(), result.getEndDate());
        verify(lockerServiceClient).occupy(10L);
        verify(lockerServiceClient, never()).release(anyLong());
    }

    @Test
    void givenLockerNotAvailable_whenCreateExceptionalRental_thenPropagatesConflictWithoutCompensating() {
        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));
        when(authServiceClient.findByUsername("jdoe")).thenReturn(new InternalUserDTO());
        when(lockerServiceClient.reserve(10L)).thenThrow(new LockerConflictException("El casillero no está disponible para reservar"));

        assertThrows(LockerConflictException.class, () -> lockerRentalService.createExceptionalRental(request));
        verify(lockerServiceClient, never()).release(anyLong());
    }

    @Test
    void givenCustomEndDateOnLockerThatDoesNotAllowIt_whenCreateExceptionalRental_thenCompensatesWithRelease() {
        request.setEndDate(LocalDateTime.now().plusDays(3));

        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));
        when(authServiceClient.findByUsername("jdoe")).thenReturn(new InternalUserDTO());

        ReserveResponseDTO reserved = new ReserveResponseDTO();
        reserved.setId(10L);
        reserved.setNumber(5);
        reserved.setBlockName("Bloque A");
        reserved.setAllowCustomRental(false);
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);

        assertThrows(IllegalStateException.class, () -> lockerRentalService.createExceptionalRental(request));

        verify(lockerServiceClient).release(10L);
        verify(lockerServiceClient, never()).occupy(any());
        verify(lockerRentalRepository, never()).save(any());
    }

    @Test
    void givenLockerServiceDownDuringOccupy_whenCreateExceptionalRental_thenPropagates503AndAttemptsCompensation() {
        when(periodService.getActivePeriod()).thenReturn(Optional.of(activePeriod));
        when(authServiceClient.findByUsername("jdoe")).thenReturn(new InternalUserDTO());

        ReserveResponseDTO reserved = new ReserveResponseDTO();
        reserved.setId(10L);
        reserved.setNumber(5);
        reserved.setBlockName("Bloque A");
        reserved.setAllowCustomRental(false);
        when(lockerServiceClient.reserve(10L)).thenReturn(reserved);
        org.mockito.Mockito.doThrow(new ServiceUnavailableException("Servicio de casilleros no disponible, intenta más tarde"))
                .when(lockerServiceClient).occupy(10L);

        assertThrows(ServiceUnavailableException.class, () -> lockerRentalService.createExceptionalRental(request));

        verify(lockerServiceClient).release(10L);
        verify(lockerRentalRepository, never()).save(any());
    }
}
