package com.raceplayback.raceplaybackserver.playback;

import com.raceplayback.raceplaybackserver.data.TelemetryPoint;
import net.minestom.server.coordinate.Pos;

public class TimelinePoint {
    private final Pos position;
    private final float yaw;
    private final long timestamp;
    private final boolean drsOpen;
    private final int gear;
    private final double speed;
    private final double throttle;
    private final boolean braking;
    
    public TimelinePoint(Pos position, float yaw, TelemetryPoint telemetry, long relativeTimestamp) {
        this.position = position;
        this.yaw = yaw;
        this.timestamp = relativeTimestamp;
        this.drsOpen = telemetry.isDRS();
        this.gear = telemetry.gear();
        this.speed = telemetry.speed().doubleValue();
        this.throttle = telemetry.throttle().doubleValue();
        this.braking = telemetry.braking();
    }
    
    public Pos getPosition() {
        return position;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isDrsOpen() {
        return drsOpen;
    }
    
    public int getGear() {
        return gear;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public double getThrottle() {
        return throttle;
    }
    
    public boolean isBraking() {
        return braking;
    }
}