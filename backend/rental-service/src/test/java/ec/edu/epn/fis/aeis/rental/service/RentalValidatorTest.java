package ec.edu.epn.fis.aeis.rental.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalValidatorTest {

    private final RentalValidator validator = new RentalValidator();

    @Test
    void givenTodayStartAndValidRange_whenValidateRentalDates_thenDoesNotThrow() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(5);

        assertDoesNotThrow(() -> validator.validateRentalDates(start, end));
    }

    @Test
    void givenStartDateNotToday_whenValidateRentalDates_thenThrows() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = start.plusDays(5);

        assertThrows(IllegalArgumentException.class, () -> validator.validateRentalDates(start, end));
    }

    @Test
    void givenEndDateBeforeStart_whenValidateRentalDates_thenThrows() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusHours(1);

        assertThrows(IllegalArgumentException.class, () -> validator.validateRentalDates(start, end));
    }

    @Test
    void givenRangeOverMaxDays_whenValidateRentalDates_thenThrows() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(20);

        assertThrows(IllegalArgumentException.class, () -> validator.validateRentalDates(start, end));
    }

    @Test
    void givenCustomDateAndAllowCustomRental_whenValidateCustomRentalSupport_thenDoesNotThrow() {
        assertDoesNotThrow(() -> validator.validateCustomRentalSupport(true, true));
    }

    @Test
    void givenNoCustomDateAndNotAllowCustomRental_whenValidateCustomRentalSupport_thenDoesNotThrow() {
        assertDoesNotThrow(() -> validator.validateCustomRentalSupport(false, false));
    }

    @Test
    void givenCustomDateButLockerDoesNotAllowIt_whenValidateCustomRentalSupport_thenThrows() {
        assertThrows(IllegalStateException.class, () -> validator.validateCustomRentalSupport(false, true));
    }

    @Test
    void givenNoCustomDateButLockerRequiresIt_whenValidateCustomRentalSupport_thenThrows() {
        assertThrows(IllegalStateException.class, () -> validator.validateCustomRentalSupport(true, false));
    }
}
