package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.model.enums.LockerRentalSetting;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RentalValidator {

    public void validateRentalDates(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime truncStart = startDate.withSecond(0).withNano(0);

        if (!truncStart.toLocalDate().isEqual(now.toLocalDate())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser hoy");
        }

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");
        }

        long daysBetween = java.time.Duration.between(truncStart, endDate.withSecond(0).withNano(0)).toDays();
        if (daysBetween > LockerRentalSetting.MAX_RENT_DAYS.getValue()) {
            throw new IllegalArgumentException("La duración del alquiler no puede exceder los "
                    + (int) LockerRentalSetting.MAX_RENT_DAYS.getValue() + " días");
        }
    }

    public void validateCustomRentalSupport(boolean allowCustomRental, boolean customEndDateProvided) {
        if (customEndDateProvided && !allowCustomRental) {
            throw new IllegalStateException("El casillero no permite alquileres personalizados");
        }

        if (!customEndDateProvided && allowCustomRental) {
            throw new IllegalStateException("El casillero permite alquileres personalizados, pero no se proporcionó una fecha de fin");
        }
    }
}
