package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.dto.PeriodRequestDTO;
import ec.edu.epn.fis.aeis.rental.exception.ResourceNotFoundException;
import ec.edu.epn.fis.aeis.rental.model.entity.Period;
import ec.edu.epn.fis.aeis.rental.repository.LockerRentalRepository;
import ec.edu.epn.fis.aeis.rental.repository.PeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    @Mock
    private PeriodRepository periodRepository;

    @Mock
    private LockerRentalRepository lockerRentalRepository;

    @InjectMocks
    private PeriodService periodService;

    private PeriodRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PeriodRequestDTO();
        validRequest.setName("2026-A");
        validRequest.setStartDate(LocalDateTime.now());
        validRequest.setEndDate(LocalDateTime.now().plusMonths(4));
    }

    @Test
    void givenValidDates_whenSaveNewPeriod_thenSavesAsInactive() {
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        Period result = periodService.saveNewPeriod(validRequest);

        assertEquals("2026-A", result.getName());
        assertFalse(result.getActive());
    }

    @Test
    void givenEndDateBeforeStartDate_whenSaveNewPeriod_thenThrows() {
        validRequest.setEndDate(validRequest.getStartDate().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> periodService.saveNewPeriod(validRequest));
        verify(periodRepository, never()).save(any());
    }

    @Test
    void givenPeriodWithoutRentals_whenUpdatePeriod_thenUpdatesFields() {
        Period existing = new Period();
        existing.setId(1L);
        existing.setName("old");
        existing.setStartDate(LocalDateTime.now());
        existing.setEndDate(LocalDateTime.now().plusDays(10));
        existing.setActive(false);

        when(periodRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(lockerRentalRepository.existsByPeriodId(1L)).thenReturn(false);
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        Period result = periodService.updatePeriod(1L, validRequest);

        assertEquals("2026-A", result.getName());
    }

    @Test
    void givenPeriodWithRentals_whenUpdatePeriod_thenThrowsConflict() {
        Period existing = new Period();
        existing.setId(1L);

        when(periodRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(lockerRentalRepository.existsByPeriodId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> periodService.updatePeriod(1L, validRequest));
        verify(periodRepository, never()).save(any());
    }

    @Test
    void givenUnknownPeriod_whenUpdatePeriod_thenThrowsNotFound() {
        when(periodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> periodService.updatePeriod(99L, validRequest));
    }

    @Test
    void givenAnotherActivePeriod_whenActivatePeriod_thenDeactivatesPreviousAndActivatesNew() {
        Period previouslyActive = new Period();
        previouslyActive.setId(1L);
        previouslyActive.setActive(true);

        Period toActivate = new Period();
        toActivate.setId(2L);
        toActivate.setActive(false);

        when(periodRepository.findByActiveTrue()).thenReturn(Optional.of(previouslyActive));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(toActivate));
        when(periodRepository.save(any(Period.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Period> captor = ArgumentCaptor.forClass(Period.class);

        Period result = periodService.activatePeriod(2L);

        assertTrue(result.getActive());
        verify(periodRepository, times(2)).save(captor.capture());
        assertFalse(captor.getAllValues().get(0).getActive());
    }
}
