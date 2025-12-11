package com.raceplayback.raceplaybackserver.mapping;

import net.minestom.server.coordinate.Pos;
import java.util.ArrayList;
import java.util.List;

public class TrackEdge {
    private final List<Pos> points;
    private final List<Double> arcLengths;
    private final double totalLength;

    public TrackEdge(List<Pos> points) {
        this.points = new ArrayList<>(points);
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
            double dist = prev.distance(curr);
            cumulative += dist;
            arcLengths.add(cumulative);
        }

        return cumulative;
    }

    public Pos getPositionAtPercent(double percent) {
        if (points.isEmpty()) {
            throw new IllegalStateException("Track edge has no points");
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

    public Pos getTangentAtPercent(double percent) {
        if (points.size() < 2) {
            return new Pos(1, 0, 0);
        }

        double epsilon = 0.001;
        Pos p1 = getPositionAtPercent(Math.max(0.0, percent - epsilon));
        Pos p2 = getPositionAtPercent(Math.min(1.0, percent + epsilon));

        double dx = p2.x() - p1.x();
        double dz = p2.z() - p1.z();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.001) {
            return new Pos(1, 0, 0);
        }

        return new Pos(dx / length, 0, dz / length);
    }

    /**
     * Find the closest point on this edge to a given position
     * @return arc-length percentage (0.0 to 1.0)
     */
    public double findClosestPercent(Pos target) {
        if (points.isEmpty()) {
            return 0.0;
        }

        double minDist = Double.MAX_VALUE;
        int closestIndex = 0;

        for (int i = 0; i < points.size(); i++) {
            double dist = points.get(i).distance(target);
            if (dist < minDist) {
                minDist = dist;
                closestIndex = i;
            }
        }

        int startIdx = Math.max(0, closestIndex - 1);
        int endIdx = Math.min(points.size() - 1, closestIndex + 1);

        double bestPercent = arcLengths.get(closestIndex) / totalLength;
        minDist = points.get(closestIndex).distance(target);

        for (int i = startIdx; i < endIdx; i++) {
            for (int j = 0; j <= 10; j++) {
                double t = j / 10.0;
                Pos p = interpolate(points.get(i), points.get(i + 1), t);
                double dist = p.distance(target);

                if (dist < minDist) {
                    minDist = dist;
                    double segmentPercent = (arcLengths.get(i) + t * (arcLengths.get(i + 1) - arcLengths.get(i))) / totalLength;
                    bestPercent = segmentPercent;
                }
            }
        }

        return bestPercent;
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

    public double getTotalLength() {
        return totalLength;
    }

    public int size() {
        return points.size();
    }
}
