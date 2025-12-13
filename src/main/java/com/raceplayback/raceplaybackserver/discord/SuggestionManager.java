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

public class SuggestionManager {
    private static final Logger logger = LoggerFactory.getLogger(SuggestionManager.class);
    private static final String SUGGESTIONS_DIR = "data/suggestions";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Suggestion> suggestionCache = new HashMap<>();

    public static void saveSuggestion(Suggestion suggestion) {
        try {
            File dir = new File(SUGGESTIONS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(SUGGESTIONS_DIR, suggestion.getSuggestionId() + ".json");

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(suggestion, writer);
            }

            suggestionCache.put(suggestion.getSuggestionId(), suggestion);

            logger.info("Saved suggestion {} to {}", suggestion.getSuggestionId(), file.getPath());

        } catch (IOException e) {
            logger.error("Failed to save suggestion {}", suggestion.getSuggestionId(), e);
        }
    }

    public static Suggestion loadSuggestion(String suggestionId) {
            if (suggestionCache.containsKey(suggestionId)) {
            logger.info("Loaded suggestion {} from cache", suggestionId);
            return suggestionCache.get(suggestionId);
        }

        try {
            File file = new File(SUGGESTIONS_DIR, suggestionId + ".json");

                if (!file.exists()) {
                logger.error("Suggestion file not found: {}", file.getPath());
                return null;
            }

            Suggestion suggestion;
            try (FileReader reader = new FileReader(file)) {
                suggestion = gson.fromJson(reader, Suggestion.class);
            }

            suggestionCache.put(suggestionId, suggestion);

            logger.info("Loaded suggestion {} from {}", suggestionId, file.getPath());

            return suggestion;

        } catch (Exception e) {
            logger.error("Failed to load suggestion {}", suggestionId, e);
            return null;
        }
    }

    public static boolean suggestionExists(String suggestionId) {
        File file = new File(SUGGESTIONS_DIR, suggestionId + ".json");
        return file.exists();
    }

    public static void clearCache() {
        suggestionCache.clear();
        logger.info("Cleared suggestion cache");
    }

    public static List<Suggestion> getAllSuggestions() {
        List<Suggestion> suggestions = new ArrayList<>();

        File dir = new File(SUGGESTIONS_DIR);
        if (!dir.exists()) {
            return suggestions;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return suggestions;
        }

        for (File file : files) {
            String suggestionId = file.getName().replace(".json", "");
            Suggestion suggestion = loadSuggestion(suggestionId);
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    public static List<Suggestion> getOpenSuggestions() {
        List<Suggestion> open = new ArrayList<>();

        for (Suggestion suggestion : getAllSuggestions()) {
            if (suggestion.getStatus().equals(SuggestionStatus.OPEN)) {
                open.add(suggestion);
            }
        }

        return open;
    }

    public static void deleteSuggestion(String suggestionId) {
        try {
            File file = new File(SUGGESTIONS_DIR, suggestionId + ".json");

            if (file.exists() && file.delete()) {
                suggestionCache.remove(suggestionId);
                logger.info("Deleted suggestion {}", suggestionId);
            } else {
                logger.warn("Suggestion file not found or could not be deleted: {}", file.getPath());
            }

        } catch (Exception e) {
            logger.error("Failed to delete suggestion {}", suggestionId, e);
        }
    }

    private static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("@", ":KM_SUPPORT_AT: ");
    }

    public static boolean sendSuggestion(Suggestion suggestion) {
        try {
            String webhookUrl = loadWebhookUrl();
            if (webhookUrl == null) {
                logger.error("Failed to load WEBHOOK_URL from .env file");
                return false;
            }

            String suggestionId = suggestion.getSuggestionId();
            String username = suggestion.getSender();
            String description = sanitizeInput(suggestion.getDescription());

            String[] words = description.split("\\s+");
            int wordCount = Math.min(10, words.length);
            String truncatedDescription = String.join(" ", java.util.Arrays.copyOfRange(words, 0, wordCount));
            if (words.length > 10) {
                truncatedDescription += "...";
            }
            String threadTitle = "[%s] %s — %s".formatted(suggestionId, username, truncatedDescription);

            if (threadTitle.length() > 100) {
                threadTitle = threadTitle.substring(0, 97) + "...";
            }

            StringBuilder threadContents = new StringBuilder();
            threadContents.append("**Suggestion ID**: ").append(suggestionId).append("\n");
            threadContents.append("**Sender**: ").append(username).append("\n");
            threadContents.append("**Sender UUID**: ").append(suggestion.getSenderUUID()).append("\n");
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
                logger.error("Failed to send suggestion to Discord. Status code: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String threadId = responseJson.get("channel_id").getAsString();

            logger.info("Suggestion {} sent to Discord. Thread ID: {}", suggestionId, threadId);

            JsonObject followUpBody = new JsonObject();
                followUpBody.addProperty("content",
                    "If you are the author of this suggestion and have images or would like to provide additional clarification, please share them here.");

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
            logger.error("Error sending suggestion to Discord", e);
            return false;
        }
    }

    private static String loadWebhookUrl() {
        try {
            String envContent = Files.readString(Paths.get(".env"));
            for (String line : envContent.split("\n")) {
                line = line.trim();
                if (line.startsWith("SUGGESTION_WEBHOOK_URL=")) {
                    String value = line.substring("SUGGESTION_WEBHOOK_URL=".length());
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
