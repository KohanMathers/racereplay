package com.raceplayback.raceplaybackserver.data;

public record LapData(
    String driverName,
    int driverNumber,
    int personalBest,
    int lapNumber,
    double lapTime,
    double sector1Time,
    double sector2Time,
    double sector3Time
) {
    public boolean isPersonalBest() {
        return (personalBest == 1);
    }
}
