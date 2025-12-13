package com.raceplayback.raceplaybackserver.commands;

import com.raceplayback.raceplaybackserver.discord.Feedback;
import com.raceplayback.raceplaybackserver.discord.FeedbackManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;

public class FeedbackCommand extends Command {

    public FeedbackCommand() {
        super("feedback");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage: /feedback <description>", NamedTextColor.RED));
        });

        ArgumentStringArray description = ArgumentType.StringArray("description");

        addSyntax((sender, context) -> {
            String[] descriptionArray = context.get("description");
            String feedbackDescription = String.join(" ", descriptionArray);

            Feedback feedback;

            if (sender instanceof Player player) {
                feedback = new Feedback(player.getUsername(), player.getUuid(), feedbackDescription);
            } else {
                feedback = new Feedback("anonymous", null, feedbackDescription);
            }

            FeedbackManager.saveFeedback(feedback);
            boolean sent = FeedbackManager.sendFeedback(feedback);

            if (sent) {
                sender.sendMessage(Component.text("Feedback created: ", NamedTextColor.GREEN)
                    .append(Component.text(feedback.getFeedbackId(), NamedTextColor.AQUA)
                    .append(Component.text(". Thank you!", NamedTextColor.GREEN))));
            } else {
                sender.sendMessage(Component.text("Failed to submit feedback", NamedTextColor.RED));
            }
        }, description);
    }
}