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

public class BugReportManager {
    private static final Logger logger = LoggerFactory.getLogger(BugReportManager.class);
    private static final String BUG_REPORTS_DIR = "data/bugreports";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, BugReport> bugReportCache = new HashMap<>();

    public static void saveBugReport(BugReport bugReport) {
        try {
            File dir = new File(BUG_REPORTS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(BUG_REPORTS_DIR, bugReport.getBugReportId() + ".json");

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(bugReport, writer);
            }

            bugReportCache.put(bugReport.getBugReportId(), bugReport);

            logger.info("Saved bug report {} to {}", bugReport.getBugReportId(), file.getPath());

        } catch (IOException e) {
            logger.error("Failed to save bug report {}", bugReport.getBugReportId(), e);
        }
    }

    public static BugReport loadBugReport(String bugReportId) {
        if (bugReportCache.containsKey(bugReportId)) {
            logger.info("Loaded bug report {} from cache", bugReportId);
            return bugReportCache.get(bugReportId);
        }

        try {
            File file = new File(BUG_REPORTS_DIR, bugReportId + ".json");

            if (!file.exists()) {
                logger.error("Bug report file not found: {}", file.getPath());
                return null;
            }

            BugReport bugReport;
            try (FileReader reader = new FileReader(file)) {
                bugReport = gson.fromJson(reader, BugReport.class);
            }

            bugReportCache.put(bugReportId, bugReport);

            logger.info("Loaded bug report {} from {}", bugReportId, file.getPath());

            return bugReport;

        } catch (Exception e) {
            logger.error("Failed to load bug report {}", bugReportId, e);
            return null;
        }
    }

    public static boolean bugReportExists(String bugReportId) {
        File file = new File(BUG_REPORTS_DIR, bugReportId + ".json");
        return file.exists();
    }

    public static void clearCache() {
        bugReportCache.clear();
        logger.info("Cleared bug report cache");
    }

    public static List<BugReport> getAllBugReports() {
        List<BugReport> bugReports = new ArrayList<>();

        File dir = new File(BUG_REPORTS_DIR);
        if (!dir.exists()) {
            return bugReports;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return bugReports;
        }

        for (File file : files) {
            String bugReportId = file.getName().replace(".json", "");
            BugReport bugReport = loadBugReport(bugReportId);
            if (bugReport != null) {
                bugReports.add(bugReport);
            }
        }

        return bugReports;
    }

    public static List<BugReport> getUnresolvedBugReports() {
        List<BugReport> unresolved = new ArrayList<>();

        for (BugReport bugReport : getAllBugReports()) {
            if (!bugReport.isResolved()) {
                unresolved.add(bugReport);
            }
        }

        return unresolved;
    }

    public static void deleteBugReport(String bugReportId) {
        try {
            File file = new File(BUG_REPORTS_DIR, bugReportId + ".json");

            if (file.exists() && file.delete()) {
                bugReportCache.remove(bugReportId);
                logger.info("Deleted bug report {}", bugReportId);
            } else {
                logger.warn("Bug report file not found or could not be deleted: {}", file.getPath());
            }

        } catch (Exception e) {
            logger.error("Failed to delete bug report {}", bugReportId, e);
        }
    }

    private static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("@", ":KM_SUPPORT_AT: ");
    }

    public static boolean sendBugReport(BugReport bugReport) {
        try {
            String webhookUrl = loadWebhookUrl();
            if (webhookUrl == null) {
                logger.error("Failed to load WEBHOOK_URL from .env file");
                return false;
            }

            String bugReportId = bugReport.getBugReportId();
            String username = bugReport.getSender();
            String description = sanitizeInput(bugReport.getDescription());
            Map<String, String> context = bugReport.getContext();

            String[] words = description.split("\\s+");
            int wordCount = Math.min(10, words.length);
            String truncatedDescription = String.join(" ", java.util.Arrays.copyOfRange(words, 0, wordCount));
            if (words.length > 10) {
                truncatedDescription += "...";
            }
            String threadTitle = "[%s] %s — %s".formatted(bugReportId, username, truncatedDescription);

            if (threadTitle.length() > 100) {
                threadTitle = threadTitle.substring(0, 97) + "...";
            }

            StringBuilder threadContents = new StringBuilder();
            threadContents.append("**Bug Report ID**: ").append(bugReportId).append("\n");
            threadContents.append("**Sender**: ").append(username).append("\n");
            threadContents.append("**Sender UUID**: ").append(bugReport.getSenderUUID()).append("\n");
            threadContents.append("**Description**: ").append(description).append("\n");

            if (context != null && !context.isEmpty()) {
                threadContents.append("\n**Context**:\n");
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    threadContents.append("**").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n");
                }
            }

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
                logger.error("Failed to send bug report to Discord. Status code: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String threadId = responseJson.get("channel_id").getAsString();

            logger.info("Bug report {} sent to Discord. Thread ID: {}", bugReportId, threadId);

            JsonObject followUpBody = new JsonObject();
            followUpBody.addProperty("content",
                    "If you are the author of this bug report and have images or would like to provide additional clarification, please share them here.");

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
            logger.error("Error sending bug report to Discord", e);
            return false;
        }
    }

    private static String loadWebhookUrl() {
        try {
            String envContent = Files.readString(Paths.get(".env"));
            for (String line : envContent.split("\n")) {
                line = line.trim();
                if (line.startsWith("BUG_REPORT_WEBHOOK_URL=")) {
                    String value = line.substring("BUG_REPORT_WEBHOOK_URL=".length());
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
