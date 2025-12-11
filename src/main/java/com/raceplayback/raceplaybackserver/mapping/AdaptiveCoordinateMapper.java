package com.raceplayback.raceplaybackserver.mapping;

import com.raceplayback.raceplaybackserver.data.TelemetryPoint;
import com.raceplayback.raceplaybackserver.mapping.RacingLineExtractor.RacingLine;
import net.minestom.server.coordinate.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdaptiveCoordinateMapper {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveCoordinateMapper.class);

    private final TrackCenterline trackCenterline;
    private RacingLine racingLine;

    private final Map<Integer, MappingResult> mappingCache = new HashMap<>();

    private final double heightOffset;
    private final boolean enableCurvatureScaling;

    public AdaptiveCoordinateMapper(TrackCenterline trackCenterline, double heightOffset) {
        this(trackCenterline, heightOffset, false);
    }

    public AdaptiveCoordinateMapper(TrackCenterline trackCenterline, double heightOffset, boolean enableCurvatureScaling) {
        this.trackCenterline = trackCenterline;
        this.heightOffset = heightOffset;
        this.enableCurvatureScaling = enableCurvatureScaling;

        logger.info("Adaptive mapper initialized with track length: {} blocks", trackCenterline.getTotalLength());
    }

    public void initializeWithTelemetry(List<TelemetryPoint> telemetry) {
        logger.info("Initializing mapper with {} telemetry points", telemetry.size());

        this.racingLine = RacingLineExtractor.extractFromTelemetry(telemetry);

        if (racingLine == null) {
            logger.error("Failed to extract racing line from telemetry");
            return;
        }

        logger.info("Racing line extracted: {} points, total length: {} units",
            racingLine.size(), racingLine.getTotalLength());

        mappingCache.clear();
    }

    public Pos mapTelemetryPoint(int telemetryIndex) {
        if (racingLine == null) {
            throw new IllegalStateException("Mapper not initialized with telemetry. Call initializeWithTelemetry() first.");
        }

        if (mappingCache.containsKey(telemetryIndex)) {
            MappingResult cached = mappingCache.get(telemetryIndex);
            return cached.position;
        }

        TelemetryPoint telemetry = racingLine.getTelemetryAt(telemetryIndex);
        if (telemetry == null) {
            logger.warn("Invalid telemetry index: {}", telemetryIndex);
            return trackCenterline.getPositionAtPercent(0.0).withY(heightOffset);
        }

        Pos telemetryPos = new Pos(
            telemetry.x().doubleValue(),
            0,
            telemetry.y().doubleValue()
        );

        int nearestIndex = racingLine.findClosestIndex(telemetryPos);

        double racingLinePercent = racingLine.getPercentAtIndex(nearestIndex);

        double lateralOffset = racingLine.calculateLateralOffset(telemetryPos, nearestIndex);

        Pos centerlinePosition = trackCenterline.getPositionAtPercent(racingLinePercent);
        Pos normal = trackCenterline.getNormalAtPercent(racingLinePercent);

        double scaledOffset = lateralOffset;
        if (enableCurvatureScaling) {
            double curvature = trackCenterline.getCurvatureAtPercent(racingLinePercent);
            double scaleFactor = computeCurvatureScale(curvature);
            scaledOffset = lateralOffset * scaleFactor;
        }

        Pos finalPosition = new Pos(
            centerlinePosition.x() + normal.x() * scaledOffset,
            heightOffset,
            centerlinePosition.z() + normal.z() * scaledOffset
        );

        MappingResult result = new MappingResult(
            finalPosition,
            racingLinePercent,
            lateralOffset,
            nearestIndex
        );
        mappingCache.put(telemetryIndex, result);

        return finalPosition;
    }

    public Pos mapCoordinates(BigDecimal telemetryX, BigDecimal telemetryY) {
        if (racingLine == null) {
            throw new IllegalStateException("Mapper not initialized with telemetry");
        }

        Pos telemetryPos = new Pos(
            telemetryX.doubleValue(),
            0,
            telemetryY.doubleValue()
        );

        int nearestIndex = racingLine.findClosestIndex(telemetryPos);
        return mapTelemetryPoint(nearestIndex);
    }

    public float calculateYaw(int telemetryIndex) {
        if (racingLine == null || telemetryIndex >= racingLine.size() - 1) {
            return 0f;
        }

        Pos current = mapTelemetryPoint(telemetryIndex);
        Pos next = mapTelemetryPoint(telemetryIndex + 1);

        double dx = next.x() - current.x();
        double dz = next.z() - current.z();

        if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
            return 0f;
        }

        double angle = Math.toDegrees(Math.atan2(dz, dx));
        float yaw = (float) (90 - angle);

        while (yaw > 180) yaw -= 360;
        while (yaw < -180) yaw += 360;

        return yaw;
    }

    private double computeCurvatureScale(double curvature) {
        double maxCurvature = 0.1;
        double scaleFactor = 1.0 - (curvature / maxCurvature) * 0.3;
        return Math.max(0.7, Math.min(1.0, scaleFactor));
    }

    public MappingResult getMappingResult(int telemetryIndex) {
        if (!mappingCache.containsKey(telemetryIndex)) {
            mapTelemetryPoint(telemetryIndex);
        }
        return mappingCache.get(telemetryIndex);
    }

    public TrackCenterline getTrackCenterline() {
        return trackCenterline;
    }

    public RacingLine getRacingLine() {
        return racingLine;
    }

    public void clearCache() {
        mappingCache.clear();
    }

    public static class MappingResult {
        public final Pos position;
        public final double trackPercent;
        public final double lateralOffset;
        public final int racingLineIndex;

        public MappingResult(Pos position, double trackPercent, double lateralOffset, int racingLineIndex) {
            this.position = position;
            this.trackPercent = trackPercent;
            this.lateralOffset = lateralOffset;
            this.racingLineIndex = racingLineIndex;
        }

        @Override
        public String toString() {
            return String.format("MappingResult{pos=(%.2f, %.2f, %.2f), percent=%.2f%%, offset=%.2f}",
                position.x(), position.y(), position.z(), trackPercent * 100, lateralOffset);
        }
    }
}
