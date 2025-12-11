package com.raceplayback.raceplaybackserver.mapping;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TrackEdgeScanner {
    private static final Logger logger = LoggerFactory.getLogger(TrackEdgeScanner.class);

    private final Instance instance;

    public TrackEdgeScanner(Instance instance) {
        this.instance = instance;
    }

    public List<Pos> scanEdge(double centerX, double centerZ, int radius, int yLevel, Block blockType) {
        logger.info("Scanning for {} blocks in radius {} around ({}, {})",
            blockType, radius, centerX, centerZ);

        List<Pos> foundBlocks = new ArrayList<>();

        int minX = (int) centerX - radius;
        int maxX = (int) centerX + radius;
        int minZ = (int) centerZ - radius;
        int maxZ = (int) centerZ + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = instance.getBlock(x, yLevel, z);
                if (block.compare(blockType)) {
                    foundBlocks.add(new Pos(x, yLevel, z));
                }
            }
        }

        logger.info("Found {} {} blocks", foundBlocks.size(), blockType);

        if (foundBlocks.isEmpty()) {
            logger.warn("No blocks found!");
            return foundBlocks;
        }

        List<Pos> orderedPath = orderIntoPath(foundBlocks);

        logger.info("Ordered {} blocks into continuous path", orderedPath.size());

        return orderedPath;
    }

    private List<Pos> orderIntoPath(List<Pos> points) {
        if (points.isEmpty()) {
            return new ArrayList<>();
        }

        List<Pos> remaining = new ArrayList<>(points);
        List<Pos> ordered = new ArrayList<>();

        Pos current = remaining.remove(0);
        ordered.add(current);

        while (!remaining.isEmpty()) {
            Pos nearest = findNearest(current, remaining);
            remaining.remove(nearest);
            ordered.add(nearest);
            current = nearest;
        }

        return ordered;
    }

    private Pos findNearest(Pos target, List<Pos> candidates) {
        return candidates.stream()
            .min(Comparator.comparingDouble(p -> p.distance(target)))
            .orElse(candidates.get(0));
    }

    public TrackEdge[] scanBothEdges(double centerX, double centerZ, int radius, int yLevel,
                                      Block leftBlockType, Block rightBlockType) {
        logger.info("Scanning for both track edges...");

        List<Pos> leftPoints = scanEdge(centerX, centerZ, radius, yLevel, leftBlockType);
        List<Pos> rightPoints = scanEdge(centerX, centerZ, radius, yLevel, rightBlockType);

        if (leftPoints.isEmpty() || rightPoints.isEmpty()) {
            logger.error("Failed to find both edges! Left: {}, Right: {}",
                leftPoints.size(), rightPoints.size());
            return null;
        }

        TrackEdge leftEdge = new TrackEdge(leftPoints);
        TrackEdge rightEdge = new TrackEdge(rightPoints);

        logger.info("Created track edges - Left: {} points ({} blocks), Right: {} points ({} blocks)",
            leftPoints.size(), leftEdge.getTotalLength(),
            rightPoints.size(), rightEdge.getTotalLength());

        double lengthRatio = leftEdge.getTotalLength() / rightEdge.getTotalLength();
        if (lengthRatio < 0.8 || lengthRatio > 1.2) {
            logger.warn("Edge lengths differ significantly! Ratio: {}", lengthRatio);
            logger.warn("This may indicate incorrect edge detection or ordering");
        }

        return new TrackEdge[] { leftEdge, rightEdge };
    }

    public void visualizeEdge(List<Pos> edge, Block markerBlock) {
        logger.info("Visualizing edge with {} points using {}", edge.size(), markerBlock);

        for (Pos pos : edge) {
            instance.setBlock(pos.add(0, 1, 0), markerBlock);
        }
    }
}
