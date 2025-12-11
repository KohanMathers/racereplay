package com.raceplayback.raceplaybackserver.mapping;

import com.raceplayback.raceplaybackserver.data.TelemetryPoint;
import net.minestom.server.coordinate.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RacingLineExtractor {
    private static final Logger logger = LoggerFactory.getLogger(RacingLineExtractor.class);

    public static RacingLine extractFromTelemetry(List<TelemetryPoint> telemetry) {
        if (telemetry == null || telemetry.isEmpty()) {
            logger.error("Cannot extract racing line from empty telemetry");
            return null;
        }

        List<Pos> points = new ArrayList<>();

        for (TelemetryPoint point : telemetry) {
            points.add(new Pos(
                point.x().doubleValue(),
                0,
                point.y().doubleValue()
            ));
        }

        logger.info("Extracted racing line with {} points from telemetry", points.size());

        return new RacingLine(points, telemetry);
    }

    public static List<TelemetryPoint> filterOffTrackPoints(List<TelemetryPoint> telemetry) {
        // TODO: Implement outlier detection
        return telemetry;
    }

    public static class RacingLine {
        private final List<Pos> points;
        private final List<TelemetryPoint> telemetry;
        private final List<Double> arcLengths;
        private final double totalLength;

        public RacingLine(List<Pos> points, List<TelemetryPoint> telemetry) {
            this.points = points;
            this.telemetry = telemetry;
            this.arcLengths = new ArrayList<>();
            this.totalLength = computeArcLengths();
        }

        private double computeArcLengths() {
            if (points.isEmpty()) {
                return 0.0;
            }

            arcLengths.add(0.0);
            double cumulative = 0.0;

            for (int i = 1; i < points.size(); i++) {
                Pos prev = points.get(i - 1);
                Pos curr = points.get(i);

                double dx = curr.x() - prev.x();
                double dz = curr.z() - prev.z();
                double dist = Math.sqrt(dx * dx + dz * dz);

                cumulative += dist;
                arcLengths.add(cumulative);
            }

            return cumulative;
        }

        public Pos getPositionAtPercent(double percent) {
            if (points.isEmpty()) {
                throw new IllegalStateException("Racing line has no points");
            }

            if (percent <= 0.0) {
                return points.get(0);
            }
            if (percent >= 1.0) {
                return points.get(points.size() - 1);
            }

            double targetLength = totalLength * percent;

            int index = 0;
            for (int i = 0; i < arcLengths.size() - 1; i++) {
                if (arcLengths.get(i) <= targetLength && targetLength <= arcLengths.get(i + 1)) {
                    index = i;
                    break;
                }
            }

            double segmentStart = arcLengths.get(index);
            double segmentEnd = arcLengths.get(index + 1);
            double segmentLength = segmentEnd - segmentStart;

            if (segmentLength < 0.001) {
                return points.get(index);
            }

            double t = (targetLength - segmentStart) / segmentLength;
            return interpolate(points.get(index), points.get(index + 1), t);
        }


        public TelemetryPoint getTelemetryAt(int index) {
            if (index < 0 || index >= telemetry.size()) {
                return null;
            }
            return telemetry.get(index);
        }

        public int findClosestIndex(Pos target) {
            if (points.isEmpty()) {
                return 0;
            }

            double minDist = Double.MAX_VALUE;
            int closestIndex = 0;

            for (int i = 0; i < points.size(); i++) {
                double dist = distance2D(points.get(i), target);
                if (dist < minDist) {
                    minDist = dist;
                    closestIndex = i;
                }
            }

            return closestIndex;
        }

        public double getPercentAtIndex(int index) {
            if (index < 0 || index >= arcLengths.size()) {
                return 0.0;
            }
            return arcLengths.get(index) / totalLength;
        }

        public double calculateLateralOffset(Pos position, int nearestIndex) {
            if (nearestIndex < 0 || nearestIndex >= points.size() - 1) {
                return 0.0;
            }

            Pos linePoint = points.get(nearestIndex);
            Pos nextPoint = points.get(nearestIndex + 1);

            double dx = nextPoint.x() - linePoint.x();
            double dz = nextPoint.z() - linePoint.z();
            double length = Math.sqrt(dx * dx + dz * dz);

            if (length < 0.001) {
                return 0.0;
            }

            dx /= length;
            dz /= length;

            double tx = position.x() - linePoint.x();
            double tz = position.z() - linePoint.z();

            return tx * (-dz) + tz * dx;
        }

        private double distance2D(Pos a, Pos b) {
            double dx = a.x() - b.x();
            double dz = a.z() - b.z();
            return Math.sqrt(dx * dx + dz * dz);
        }

        private Pos interpolate(Pos a, Pos b, double t) {
            return new Pos(
                a.x() + (b.x() - a.x()) * t,
                a.y() + (b.y() - a.y()) * t,
                a.z() + (b.z() - a.z()) * t
            );
        }

        public List<Pos> getPoints() {
            return new ArrayList<>(points);
        }

        public List<TelemetryPoint> getTelemetry() {
            return new ArrayList<>(telemetry);
        }

        public double getTotalLength() {
            return totalLength;
        }

        public int size() {
            return points.size();
        }
    }
}
