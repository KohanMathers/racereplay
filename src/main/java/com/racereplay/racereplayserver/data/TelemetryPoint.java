package com.racereplay.racereplayserver.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TelemetryPoint(
    boolean braking,
    Compound compound,
    int drs,
    BigDecimal distance,
    BigDecimal rpm,
    Long sessionTime,
    BigDecimal speed,
    BigDecimal throttle,
    BigDecimal x,
    BigDecimal y,
    int gear
) {
    public TelemetryPoint {
        distance = distance.setScale(4, RoundingMode.HALF_UP);
        rpm = rpm.setScale(4, RoundingMode.HALF_UP);
        speed = speed.setScale(4, RoundingMode.HALF_UP);
        throttle = throttle.setScale(4, RoundingMode.HALF_UP);
        x = x.setScale(4, RoundingMode.HALF_UP);
        y = y.setScale(4, RoundingMode.HALF_UP);
    }

    public boolean isDRS() {
        return (drs == 10 || drs == 12 || drs == 14);
    }
}
