package com.raceplayback.raceplaybackserver.commands;

import com.raceplayback.raceplaybackserver.discord.BugReport;
import com.raceplayback.raceplaybackserver.discord.BugReportManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;

public class ReportBugCommand extends Command {

    public ReportBugCommand() {
        super("reportbug");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage: /reportbug <description>", NamedTextColor.RED));
        });

        ArgumentStringArray description = ArgumentType.StringArray("description");

        addSyntax((sender, context) -> {
            String[] descriptionArray = context.get("description");
            String bugDescription = String.join(" ", descriptionArray);

            BugReport bugReport;

            if (sender instanceof Player player) {
                bugReport = new BugReport(player.getUsername(), player.getUuid(), bugDescription, null);
            } else {
                bugReport = new BugReport("anonymous", null, bugDescription, null);
            }

            BugReportManager.saveBugReport(bugReport);
            boolean sent = BugReportManager.sendBugReport(bugReport);

            if (sent) {
                sender.sendMessage(Component.text("Bug report created: ", NamedTextColor.GREEN)
                    .append(Component.text(bugReport.getBugReportId(), NamedTextColor.AQUA)
                    .append(Component.text(" [", NamedTextColor.GREEN)
                    .append(Component.text("View the report tracker here", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://discord.com/channels/1373880732413595678/1373883557692444732")))
                    .append(Component.text("]", NamedTextColor.GREEN)))));
            } else {
                sender.sendMessage(Component.text("Failed to submit bug report", NamedTextColor.RED));
            }
        }, description);
    }
}