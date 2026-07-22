package ec.edu.epn.fis.aeis.rental.model.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum LockerRentalSetting {

    MAX_RENT_DAYS(15),
    PERIOD_RENT_PRICE(6.5),
    CUSTOM_RENT_DAILY_PRICE(1);

    private final double value;

    LockerRentalSetting(double value) {
        this.value = value;
    }

    public BigDecimal getValueAsBigDecimal() {
        return BigDecimal.valueOf(value);
    }
}
