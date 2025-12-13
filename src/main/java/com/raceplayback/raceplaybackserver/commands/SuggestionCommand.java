package com.raceplayback.raceplaybackserver.commands;

import com.raceplayback.raceplaybackserver.discord.Suggestion;
import com.raceplayback.raceplaybackserver.discord.SuggestionManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;

public class SuggestionCommand extends Command {

    public SuggestionCommand() {
        super("suggest");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage: /suggest <description>", NamedTextColor.RED));
        });

        ArgumentStringArray description = ArgumentType.StringArray("description");

        addSyntax((sender, context) -> {
            String[] descriptionArray = context.get("description");
            String suggestionDescription = String.join(" ", descriptionArray);

            Suggestion suggestion;

            if (sender instanceof Player player) {
                suggestion = new Suggestion(player.getUsername(), player.getUuid(), suggestionDescription);
            } else {
                suggestion = new Suggestion("anonymous", null, suggestionDescription);
            }

            SuggestionManager.saveSuggestion(suggestion);
            boolean sent = SuggestionManager.sendSuggestion(suggestion);

            if (sent) {
                sender.sendMessage(Component.text("Suggestion created: ", NamedTextColor.GREEN)
                    .append(Component.text(suggestion.getSuggestionId(), NamedTextColor.AQUA)
                    .append(Component.text(" [", NamedTextColor.GREEN)
                    .append(Component.text("View the suggestion tracker here", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://discord.com/channels/1373880732413595678/1373883557692444732")))
                    .append(Component.text("]", NamedTextColor.GREEN)))));
            } else {
                sender.sendMessage(Component.text("Failed to submit suggestion", NamedTextColor.RED));
            }
        }, description);
    }
}