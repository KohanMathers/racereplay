package com.raceplayback.raceplaybackserver.discord;

import java.util.UUID;

public class Feedback {
    private String sender;
    private UUID senderUUID;
    private String description;
    private String feedbackId;

    public Feedback(String sender, UUID senderUUID, String description) {
        this.sender = sender;
        this.senderUUID = senderUUID;
        this.description = description;
        this.feedbackId = UUID.randomUUID().toString().split("-")[0];
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

    public String getFeedbackId() {
        return feedbackId;
    }

}
