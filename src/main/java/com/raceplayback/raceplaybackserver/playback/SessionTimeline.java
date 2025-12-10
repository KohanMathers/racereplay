package com.raceplayback.raceplaybackserver.playback;

import com.raceplayback.raceplaybackserver.data.TelemetryPoint;
import com.raceplayback.raceplaybackserver.util.CoordinateConverter;
import net.minestom.server.coordinate.Pos;

import java.util.ArrayList;
import java.util.List;

public class SessionTimeline {
    private final List<TimelinePoint> points;
    private final CoordinateConverter converter;
    private final double trackHeight;
    
    private int currentIndex = 0;
    private long startTime = 0;
    private boolean playing = false;
    
    public SessionTimeline(CoordinateConverter converter, double trackHeight) {
        this.points = new ArrayList<>();
        this.converter = converter;
        this.trackHeight = trackHeight;
    }
    
    public void buildFromTelemetry(List<TelemetryPoint> telemetry) {
        points.clear();

        if (telemetry.isEmpty()) return;

        long baseTimestamp = telemetry.get(0).sessionTime();

        for (int i = 0; i < telemetry.size(); i++) {
            TelemetryPoint current = telemetry.get(i);

            Pos position = converter.toMinecraftPos(
                current.x(),
                current.y(),
                trackHeight
            );

            float yaw = 0;
            if (i < telemetry.size() - 1) {
                TelemetryPoint next = telemetry.get(i + 1);
                Pos nextPos = converter.toMinecraftPos(
                    next.x(),
                    next.y(),
                    trackHeight
                );

                if (i < 5) {
                    System.out.println(String.format("[TIMELINE] Point %d: Tel(%.2f, %.2f) -> MC(%.2f, %.2f)",
                        i, current.x(), current.y(), position.x(), position.z()));
                    System.out.println(String.format("[TIMELINE] Point %d Next: Tel(%.2f, %.2f) -> MC(%.2f, %.2f)",
                        i, next.x(), next.y(), nextPos.x(), nextPos.z()));
                }

                yaw = converter.calculateYaw(position, nextPos);

                if (i < 5) {
                    System.out.println(String.format("[TIMELINE] Point %d: Calculated yaw = %.2f°", i, yaw));
                }
            } else if (i > 0) {
                yaw = points.get(i - 1).getYaw();
            }

            TimelinePoint point = new TimelinePoint(
                position,
                yaw,
                current,
                current.sessionTime() - baseTimestamp
            );
            points.add(point);
        }

        System.out.println("Timeline built: " + points.size() + " points, duration: " +
            points.get(points.size() - 1).getTimestamp() + "ms"
        );
    }
    
    public void start() {
        this.playing = true;
        this.startTime = System.currentTimeMillis();
        this.currentIndex = 0;
    }

    public void stop() {
        this.playing = false;
    }

    public TimelinePoint getCurrentPoint() {
        if (!playing || points.isEmpty()) {
            return null;
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        while (currentIndex < points.size()) {
            TimelinePoint point = points.get(currentIndex);
            
            if (point.getTimestamp() <= elapsed) {
                currentIndex++;
                return point;
            } else {
                break;
            }
        }
        
        if (currentIndex >= points.size()) {
            stop();
            return null;
        }
        
        return null;
    }
    
    public boolean isFinished() {
        return !playing || currentIndex >= points.size();
    }
    
    public boolean isPlaying() {
        return playing;
    }
    
    public int getTotalPoints() {
        return points.size();
    }
    
    public int getCurrentIndex() {
        return currentIndex;
    }
}