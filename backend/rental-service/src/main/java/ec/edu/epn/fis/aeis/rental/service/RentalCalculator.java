package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.model.enums.LockerRentalSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class RentalCalculator {

    private static final Logger log = LoggerFactory.getLogger(RentalCalculator.class);

    public BigDecimal calculateCustomRentalAmount(LocalDateTime startDate, LocalDateTime endDate) {
        long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1; // Incluye el día de inicio
        BigDecimal dailyPrice = LockerRentalSetting.CUSTOM_RENT_DAILY_PRICE.getValueAsBigDecimal();
        BigDecimal amountToPay = dailyPrice.multiply(BigDecimal.valueOf(days));

        log.info("Calculando monto de renta personalizada: precio diario = {}, días = {}, total = {}",
                dailyPrice, days, amountToPay);

        return amountToPay;
    }
}
