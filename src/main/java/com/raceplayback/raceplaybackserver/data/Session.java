package com.raceplayback.raceplaybackserver.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Session(
    String circuitName,
    LocalDateTime date,
    String grandPrix,
    int numberOfLaps,    
    SessionType sessionType,
    int year
) {
    public Session {
        if (year < 2018) {
            throw new IllegalArgumentException("RaceReplay does not support below 2018 yet");
        }

        grandPrix = grandPrix.substring(0, 1).toUpperCase() + grandPrix.substring(1);
    }

    public boolean isRace() {
        return (sessionType.equals(SessionType.R) || sessionType.equals(SessionType.S));
    }

    public boolean isQuali() {
        return (sessionType.equals(SessionType.Q) || sessionType.equals(SessionType.SQ));
    }

    public boolean isPractice() {
        return (sessionType.equals(SessionType.FP1) ||  sessionType.equals(SessionType.FP2) || sessionType.equals(SessionType.FP3));
    }

    public boolean isSprint() {
        return (sessionType.equals(SessionType.S) || sessionType.equals(SessionType.SQ));
    }

    public String fullName() {
        return "%s Grand Prix %d - %s - %s".formatted(grandPrix, year, sessionType.getTitleCase(), date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }
}