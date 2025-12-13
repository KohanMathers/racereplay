package com.raceplayback.raceplaybackserver.discord;

import java.util.UUID;

public class Suggestion {
    private String sender;
    private UUID senderUUID;
    private String description;
    private String suggestionId;
    private SuggestionStatus status;

    public Suggestion(String sender, UUID senderUUID, String description) {
        this.sender = sender;
        this.senderUUID = senderUUID;
        this.description = description;
        this.suggestionId = UUID.randomUUID().toString().split("-")[0];
        this.status = SuggestionStatus.OPEN;
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

    public String getSuggestionId() {
        return suggestionId;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }
}
