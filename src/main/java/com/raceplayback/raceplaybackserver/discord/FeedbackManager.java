package com.raceplayback.raceplaybackserver.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackManager {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackManager.class);
    private static final String FEEDBACKS_DIR = "data/feedbacks";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Feedback> feedbackCache = new HashMap<>();

    public static void saveFeedback(Feedback feedback) {
        try {
            File dir = new File(FEEDBACKS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(FEEDBACKS_DIR, feedback.getFeedbackId() + ".json");

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(feedback, writer);
            }

            feedbackCache.put(feedback.getFeedbackId(), feedback);

            logger.info("Saved feedback {} to {}", feedback.getFeedbackId(), file.getPath());

        } catch (IOException e) {
            logger.error("Failed to save feedback {}", feedback.getFeedbackId(), e);
        }
    }

    public static Feedback loadFeedback(String feedbackId) {
            if (feedbackCache.containsKey(feedbackId)) {
            logger.info("Loaded feedback {} from cache", feedbackId);
            return feedbackCache.get(feedbackId);
        }

        try {
            File file = new File(FEEDBACKS_DIR, feedbackId + ".json");

                if (!file.exists()) {
                logger.error("Feedback file not found: {}", file.getPath());
                return null;
            }

            Feedback feedback;
            try (FileReader reader = new FileReader(file)) {
                feedback = gson.fromJson(reader, Feedback.class);
            }

            feedbackCache.put(feedbackId, feedback);

            logger.info("Loaded feedback {} from {}", feedbackId, file.getPath());

            return feedback;

        } catch (Exception e) {
            logger.error("Failed to load feedback {}", feedbackId, e);
            return null;
        }
    }

    public static boolean feedbackExists(String feedbackId) {
        File file = new File(FEEDBACKS_DIR, feedbackId + ".json");
        return file.exists();
    }

    public static void clearCache() {
        feedbackCache.clear();
        logger.info("Cleared feedback cache");
    }

    public static List<Feedback> getAllFeedbacks() {
        List<Feedback> feedbacks = new ArrayList<>();

        File dir = new File(FEEDBACKS_DIR);
        if (!dir.exists()) {
            return feedbacks;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return feedbacks;
        }

        for (File file : files) {
            String feedbackId = file.getName().replace(".json", "");
            Feedback feedback = loadFeedback(feedbackId);
            if (feedback != null) {
                feedbacks.add(feedback);
            }
        }

        return feedbacks;
    }

    public static void deleteFeedback(String feedbackId) {
        try {
            File file = new File(FEEDBACKS_DIR, feedbackId + ".json");

            if (file.exists() && file.delete()) {
                feedbackCache.remove(feedbackId);
                logger.info("Deleted feedback {}", feedbackId);
            } else {
                logger.warn("Feedback file not found or could not be deleted: {}", file.getPath());
            }

        } catch (Exception e) {
            logger.error("Failed to delete feedback {}", feedbackId, e);
        }
    }

    private static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("@", ":KM_SUPPORT_AT: ");
    }

    public static boolean sendFeedback(Feedback feedback) {
        try {
            String webhookUrl = loadWebhookUrl();
            if (webhookUrl == null) {
                logger.error("Failed to load WEBHOOK_URL from .env file");
                return false;
            }

            String feedbackId = feedback.getFeedbackId();
            String username = feedback.getSender();
            String description = sanitizeInput(feedback.getDescription());

            String[] words = description.split("\\s+");
            int wordCount = Math.min(10, words.length);
            String truncatedDescription = String.join(" ", java.util.Arrays.copyOfRange(words, 0, wordCount));
            if (words.length > 10) {
                truncatedDescription += "...";
            }
            String threadTitle = "[%s] %s — %s".formatted(feedbackId, username, truncatedDescription);

            if (threadTitle.length() > 100) {
                threadTitle = threadTitle.substring(0, 97) + "...";
            }

            StringBuilder threadContents = new StringBuilder();
            threadContents.append("**Feedback ID**: ").append(feedbackId).append("\n");
            threadContents.append("**Sender**: ").append(username).append("\n");
            threadContents.append("**Sender UUID**: ").append(feedback.getSenderUUID()).append("\n");
            threadContents.append("**Description**: ").append(description).append("\n");

            HttpClient client = HttpClient.newHttpClient();

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("thread_name", threadTitle);
            requestBody.addProperty("content", threadContents.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl + "?wait=true"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Failed to send feedback to Discord. Status code: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String threadId = responseJson.get("channel_id").getAsString();

            logger.info("Feedback {} sent to Discord. Thread ID: {}", feedbackId, threadId);

            JsonObject followUpBody = new JsonObject();
                followUpBody.addProperty("content",
                    "If you are the author of this feedback and have images or would like to provide additional clarification, please share them here.");

            HttpRequest followUpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl + "?thread_id=" + threadId))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(followUpBody)))
                    .build();

            HttpResponse<String> followUpResponse = client.send(followUpRequest, HttpResponse.BodyHandlers.ofString());

            if (followUpResponse.statusCode() != 204) {
                logger.warn("Failed to send follow-up message to thread {}. Status code: {}",
                        threadId, followUpResponse.statusCode());
            }

            return true;

        } catch (Exception e) {
            logger.error("Error sending feedback to Discord", e);
            return false;
        }
    }

    private static String loadWebhookUrl() {
        try {
            String envContent = Files.readString(Paths.get(".env"));
            for (String line : envContent.split("\n")) {
                line = line.trim();
                if (line.startsWith("FEEDBACK_WEBHOOK_URL=")) {
                    String value = line.substring("FEEDBACK_WEBHOOK_URL=".length());
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read .env file", e);
        }
        return null;
    }
}
