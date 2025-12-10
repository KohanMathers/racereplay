package com.raceplayback.raceplaybackserver.playback;

import com.raceplayback.raceplaybackserver.RacePlaybackServer;
import com.raceplayback.raceplaybackserver.data.Compound;
import com.raceplayback.raceplaybackserver.data.DataModelType;
import com.raceplayback.raceplaybackserver.data.Session;
import com.raceplayback.raceplaybackserver.data.SessionType;
import com.raceplayback.raceplaybackserver.data.TelemetryPoint;
import com.raceplayback.raceplaybackserver.data.TrackName;
import com.raceplayback.raceplaybackserver.entity.car.F1Car;
import com.raceplayback.raceplaybackserver.network.F1ApiClient;
import com.raceplayback.raceplaybackserver.util.CoordinateConverter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

public class PlaybackController {
    private final RacePlaybackServer server = RacePlaybackServer.getInstance();

    private F1Car car;
    private SessionTimeline currentTimeline;
    private SessionTimeline nextTimeline;
    private CoordinateConverter converter;

    private int year;
    private TrackName track;
    private SessionType sessionType;
    private String driverCode;
    private int currentLap = 1;
    private int totalLaps;
    private double rotationOffset;

    private boolean running = false;

    public PlaybackController(int year, TrackName track, SessionType sessionType, String driverCode, Pos startPosition, double rotationOffset, Instance instance) {
        this.year = year;
        this.track = track;
        this.sessionType = sessionType;
        this.driverCode = driverCode;
        this.rotationOffset = rotationOffset;
        this.converter = new CoordinateConverter(startPosition, rotationOffset);
        
        F1ApiClient sessionClient = new F1ApiClient(
            "https://raceplayback.com/api/v1/sessions",
            year,
            track,
            sessionType,
            "info",
            DataModelType.SESSION
        );
        
        Session session = (Session) sessionClient.getData();
        this.totalLaps = session != null ? session.numberOfLaps() : 57;
        
        server.getLogger().info("Session has {} laps", totalLaps);
    }
    
    public void initialize(Instance instance) {
        server.getLogger().info("Loading lap 1 for driver {}...", driverCode);

        List<TelemetryPoint> telemetry = fetchTelemetry(currentLap);

        if (telemetry == null || telemetry.isEmpty()) {
            server.getLogger().error("No telemetry data for lap 1!");
            return;
        }

        Compound compound = telemetry.get(0).compound();

        car = new F1Car(driverCode, compound);
        Pos startPos = converter.toMinecraftPos(
            telemetry.get(0).x(),
            telemetry.get(0).y(),
            42
        );
        car.spawn(instance, startPos);

        currentTimeline = new SessionTimeline(converter, 42);
        currentTimeline.buildFromTelemetry(telemetry);

        server.getLogger().info("Lap 1 loaded with {} telemetry points", telemetry.size());

        preloadNextLap();
    }
    
    public void startWithCountdown(Instance instance) {
        server.getLogger().info("Starting countdown...");
        
        final int[] countdown = {15};
        final boolean[] hasStarted = {false};
        
        instance.scheduler().buildTask(() -> {
            if (hasStarted[0]) return;
            
            if (countdown[0] > 0) {
                server.getLogger().info("Starting in {}...", countdown[0]);
                countdown[0]--;
            } else {
                hasStarted[0] = true;
                start(instance);
            }
        }).repeat(TaskSchedule.seconds(1)).schedule();
    }
    
    private void start(Instance instance) {
        running = true;
        currentTimeline.start();
        
        server.getLogger().info("Replay started! Timeline has {} points", 
            currentTimeline.getTotalPoints());
        
        instance.scheduler().buildTask(() -> {
            if (!running) return;
            
            update();
            
            if (currentTimeline.isFinished()) {
                onLapFinished(instance);
            }
        }).repeat(TaskSchedule.millis(50)).schedule();
    }
    
    private void update() {
        TimelinePoint point = currentTimeline.getCurrentPoint();
        
        if (point != null) {
            if (currentTimeline.getCurrentIndex() < 5) {
                server.getLogger().info("Update {}: pos={}, yaw={}, timestamp={}ms", 
                    currentTimeline.getCurrentIndex(),
                    point.getPosition(),
                    point.getYaw(),
                    point.getTimestamp()
                );
            }
            
            Pos posWithYaw = point.getPosition().withYaw(point.getYaw());
            
            car.update(posWithYaw);
            
            car.setDRS(point.isDrsOpen());
            
            float steeringAngle = point.getYaw() * 0.3f;
            car.setSteeringAngle(steeringAngle);
        }
    }
    
    private void onLapFinished(Instance instance) {
        server.getLogger().info("Lap {} finished!", currentLap);
        
        currentLap++;
        
        if (currentLap > totalLaps) {
            server.getLogger().info("Session finished!");
            stop();
            return;
        }
        
        if (nextTimeline != null) {
            currentTimeline = nextTimeline;
            currentTimeline.start();
            
            server.getLogger().info("Starting lap {}", currentLap);
            
            preloadNextLap();
        } else {
            server.getLogger().error("Next lap not preloaded!");
            stop();
        }
    }
    
    private void preloadNextLap() {
        if (currentLap >= totalLaps) {
            return;
        }
        
        int nextLap = currentLap + 1;
        
        server.getLogger().info("Preloading lap {}...", nextLap);
        
        List<TelemetryPoint> telemetry = fetchTelemetry(nextLap);
        
        if (telemetry != null && !telemetry.isEmpty()) {
            CoordinateConverter lapConverter = new CoordinateConverter(car.getPosition(), rotationOffset);
            
            nextTimeline = new SessionTimeline(lapConverter, 42);
            nextTimeline.buildFromTelemetry(telemetry);
            server.getLogger().info("Lap {} preloaded with {} points", nextLap, telemetry.size());
        } else {
            server.getLogger().warn("Failed to preload lap {}", nextLap);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<TelemetryPoint> fetchTelemetry(int lap) {
        String endpoint = String.format("telemetry/%s/%d", driverCode, lap);
        
        F1ApiClient client = new F1ApiClient(
            "https://raceplayback.com/api/v1/sessions",
            year,
            track,
            sessionType,
            endpoint,
            DataModelType.TELEMETRY_POINT
        );
        
        return (List<TelemetryPoint>) client.getData();
    }
    
    public void stop() {
        running = false;
        if (currentTimeline != null) {
            currentTimeline.stop();
        }
        if (car != null) {
            car.remove();
        }
    }

    public boolean isRunning() {
        return running;
    }
}