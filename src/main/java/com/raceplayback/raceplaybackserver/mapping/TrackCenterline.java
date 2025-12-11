package com.raceplayback.raceplaybackserver.mapping;

import net.minestom.server.coordinate.Pos;
import java.util.ArrayList;
import java.util.List;

public class TrackCenterline {
    private final TrackEdge leftEdge;
    private final TrackEdge rightEdge;
    private final List<Pos> centerlinePoints;
    private final List<Double> arcLengths;
    private final List<Double> trackWidths;
    private final double totalLength;

    public TrackCenterline(TrackEdge leftEdge, TrackEdge rightEdge) {
        this.leftEdge = leftEdge;
        this.rightEdge = rightEdge;
        this.centerlinePoints = new ArrayList<>();
        this.arcLengths = new ArrayList<>();
        this.trackWidths = new ArrayList<>();
        this.totalLength = computeCenterline();
    }

    private double computeCenterline() {
        int samples = 1000;

        for (int i = 0; i <= samples; i++) {
            double percent = i / (double) samples;

            Pos leftPoint = leftEdge.getPositionAtPercent(percent);
            Pos rightPoint = rightEdge.getPositionAtPercent(percent);

            Pos centerPoint = new Pos(
                (leftPoint.x() + rightPoint.x()) / 2.0,
                (leftPoint.y() + rightPoint.y()) / 2.0,
                (leftPoint.z() + rightPoint.z()) / 2.0
            );

            centerlinePoints.add(centerPoint);

            double width = leftPoint.distance(rightPoint);
            trackWidths.add(width);
        }

        arcLengths.add(0.0);
        double cumulative = 0.0;

        for (int i = 1; i < centerlinePoints.size(); i++) {
            Pos prev = centerlinePoints.get(i - 1);
            Pos curr = centerlinePoints.get(i);
            double dist = prev.distance(curr);
            cumulative += dist;
            arcLengths.add(cumulative);
        }

        return cumulative;
    }

    public Pos getPositionAtPercent(double percent) {
        if (centerlinePoints.isEmpty()) {
            throw new IllegalStateException("Centerline has no points");
        }

        if (percent <= 0.0) {
            return centerlinePoints.get(0);
        }
        if (percent >= 1.0) {
            return centerlinePoints.get(centerlinePoints.size() - 1);
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
            return centerlinePoints.get(index);
        }

        double t = (targetLength - segmentStart) / segmentLength;
        return interpolate(centerlinePoints.get(index), centerlinePoints.get(index + 1), t);
    }

    public Pos getNormalAtPercent(double percent) {
        Pos tangent = getTangentAtPercent(percent);

        return new Pos(-tangent.z(), 0, tangent.x());
    }

    public Pos getTangentAtPercent(double percent) {
        if (centerlinePoints.size() < 2) {
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

    public double findClosestPercent(Pos target) {
        if (centerlinePoints.isEmpty()) {
            return 0.0;
        }

        double minDist = Double.MAX_VALUE;
        int closestIndex = 0;

        for (int i = 0; i < centerlinePoints.size(); i++) {
            double dist = centerlinePoints.get(i).distance(target);
            if (dist < minDist) {
                minDist = dist;
                closestIndex = i;
            }
        }

        return arcLengths.get(closestIndex) / totalLength;
    }

    public double getTrackWidthAtPercent(double percent) {
        if (trackWidths.isEmpty()) {
            return 10.0;
        }

        int index = (int) (percent * (trackWidths.size() - 1));
        index = Math.max(0, Math.min(trackWidths.size() - 1, index));

        return trackWidths.get(index);
    }

    public double getCurvatureAtPercent(double percent) {
        double epsilon = 0.01;

        Pos t1 = getTangentAtPercent(Math.max(0.0, percent - epsilon));
        Pos t2 = getTangentAtPercent(Math.min(1.0, percent + epsilon));

        double dot = t1.x() * t2.x() + t1.z() * t2.z();
        dot = Math.max(-1.0, Math.min(1.0, dot));

        double angle = Math.acos(dot);
        double distance = totalLength * 2 * epsilon;

        if (distance < 0.001) {
            return 0.0;
        }

        return angle / distance;
    }

    private Pos interpolate(Pos a, Pos b, double t) {
        return new Pos(
            a.x() + (b.x() - a.x()) * t,
            a.y() + (b.y() - a.y()) * t,
            a.z() + (b.z() - a.z()) * t
        );
    }

    public double getTotalLength() {
        return totalLength;
    }

    public List<Pos> getCenterlinePoints() {
        return new ArrayList<>(centerlinePoints);
    }

    public TrackEdge getLeftEdge() {
        return leftEdge;
    }

    public TrackEdge getRightEdge() {
        return rightEdge;
    }
}
