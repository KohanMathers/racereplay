package com.raceplayback.raceplaybackserver.data;

import java.net.URI;
import java.time.LocalDateTime;

public record TeamRadio(
    URI audioUrl,
    int driverNumber,
    Long timestamp,
    String transcript,
    LocalDateTime utc
) {}
