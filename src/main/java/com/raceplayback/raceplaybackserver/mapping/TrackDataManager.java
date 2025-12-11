package com.raceplayback.raceplaybackserver.mapping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.raceplayback.raceplaybackserver.data.TrackName;
import net.minestom.server.coordinate.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackDataManager {
    private static final Logger logger = LoggerFactory.getLogger(TrackDataManager.class);
    private static final String TRACKS_DIR = "data/tracks";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<TrackName, TrackCenterline> centerlineCache = new HashMap<>();

    public static void saveTrackEdges(TrackName trackName, TrackEdge leftEdge, TrackEdge rightEdge) {
        try {
            File dir = new File(TRACKS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(TRACKS_DIR, trackName.name().toLowerCase() + ".json");

            JsonObject json = new JsonObject();
            json.addProperty("track", trackName.name());

            JsonArray leftArray = new JsonArray();
            for (Pos pos : leftEdge.getPoints()) {
                JsonObject point = new JsonObject();
                point.addProperty("x", pos.x());
                point.addProperty("y", pos.y());
                point.addProperty("z", pos.z());
                leftArray.add(point);
            }
            json.add("leftEdge", leftArray);

            JsonArray rightArray = new JsonArray();
            for (Pos pos : rightEdge.getPoints()) {
                JsonObject point = new JsonObject();
                point.addProperty("x", pos.x());
                point.addProperty("y", pos.y());
                point.addProperty("z", pos.z());
                rightArray.add(point);
            }
            json.add("rightEdge", rightArray);

            json.addProperty("leftEdgeLength", leftEdge.getTotalLength());
            json.addProperty("rightEdgeLength", rightEdge.getTotalLength());
            json.addProperty("leftEdgePoints", leftEdge.size());
            json.addProperty("rightEdgePoints", rightEdge.size());

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(json, writer);
            }

            logger.info("Saved track edges for {} to {}", trackName, file.getPath());
            logger.info("  Left edge: {} points, {} blocks", leftEdge.size(), leftEdge.getTotalLength());
            logger.info("  Right edge: {} points, {} blocks", rightEdge.size(), rightEdge.getTotalLength());

        } catch (IOException e) {
            logger.error("Failed to save track edges for {}", trackName, e);
        }
    }

    public static TrackCenterline loadTrackCenterline(TrackName trackName) {
        if (centerlineCache.containsKey(trackName)) {
            logger.info("Loaded {} centerline from cache", trackName);
            return centerlineCache.get(trackName);
        }

        try {
            File file = new File(TRACKS_DIR, trackName.name().toLowerCase() + ".json");

            if (!file.exists()) {
                logger.error("Track data file not found: {}", file.getPath());
                logger.error("Please scan the track first using /scantack command");
                return null;
            }

            JsonObject json;
            try (FileReader reader = new FileReader(file)) {
                json = gson.fromJson(reader, JsonObject.class);
            }

            JsonArray leftArray = json.getAsJsonArray("leftEdge");
            List<Pos> leftPoints = new ArrayList<>();
            for (int i = 0; i < leftArray.size(); i++) {
                JsonObject point = leftArray.get(i).getAsJsonObject();
                leftPoints.add(new Pos(
                    point.get("x").getAsDouble(),
                    point.get("y").getAsDouble(),
                    point.get("z").getAsDouble()
                ));
            }

            JsonArray rightArray = json.getAsJsonArray("rightEdge");
            List<Pos> rightPoints = new ArrayList<>();
            for (int i = 0; i < rightArray.size(); i++) {
                JsonObject point = rightArray.get(i).getAsJsonObject();
                rightPoints.add(new Pos(
                    point.get("x").getAsDouble(),
                    point.get("y").getAsDouble(),
                    point.get("z").getAsDouble()
                ));
            }

            TrackEdge leftEdge = new TrackEdge(leftPoints);
            TrackEdge rightEdge = new TrackEdge(rightPoints);
            TrackCenterline centerline = new TrackCenterline(leftEdge, rightEdge);

            centerlineCache.put(trackName, centerline);

            logger.info("Loaded {} centerline from {}", trackName, file.getPath());
            logger.info("  Centerline length: {} blocks", centerline.getTotalLength());

            return centerline;

        } catch (Exception e) {
            logger.error("Failed to load track centerline for {}", trackName, e);
            return null;
        }
    }

    public static boolean trackDataExists(TrackName trackName) {
        File file = new File(TRACKS_DIR, trackName.name().toLowerCase() + ".json");
        return file.exists();
    }

    public static void clearCache() {
        centerlineCache.clear();
        logger.info("Cleared track centerline cache");
    }

    public static List<TrackName> getAvailableTracks() {
        List<TrackName> available = new ArrayList<>();

        File dir = new File(TRACKS_DIR);
        if (!dir.exists()) {
            return available;
        }

        for (TrackName track : TrackName.values()) {
            if (trackDataExists(track)) {
                available.add(track);
            }
        }

        return available;
    }
}
