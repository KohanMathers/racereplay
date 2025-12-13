package com.raceplayback.raceplaybackserver.discord;

import java.util.Map;
import java.util.UUID;

public class BugReport {
    private String sender;
    private UUID senderUUID;
    private String description;
    private Map<String, String> context;
    private String bugReportId;
    private boolean resolved;

    public BugReport(String sender, UUID senderUUID, String description, Map<String, String> context) {
        this.sender = sender;
        this.senderUUID = senderUUID;
        this.description = description;
        this.context = context;
        this.bugReportId = UUID.randomUUID().toString().split("-")[0];
        this.resolved = false;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public void setSenderUUID(UUID senderUUID) {
        this.senderUUID = senderUUID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public String getBugReportId() {
        return bugReportId;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void resolve() {
        this.resolved = true;
    }
}
