package com.racereplay.racereplayserver.data;

public record LapData(
    boolean isPersonalBest,
    int lapNumber,
    double lapTime,
    double sector1Time,
    double sector2Time,
    double sector3Time
) {}
