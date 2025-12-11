package com.raceplayback.raceplaybackserver.commands;

import com.raceplayback.raceplaybackserver.data.TrackName;
import com.raceplayback.raceplaybackserver.mapping.TrackCenterline;
import com.raceplayback.raceplaybackserver.mapping.TrackDataManager;
import com.raceplayback.raceplaybackserver.mapping.TrackEdge;
import com.raceplayback.raceplaybackserver.mapping.TrackEdgeScanner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class ScanTrackCommand extends Command {
    private final Instance instance;

    public ScanTrackCommand(Instance instance) {
        super("scantrack");
        this.instance = instance;

        var trackArg = ArgumentType.Enum("track", TrackName.class);
        var radiusArg = ArgumentType.Integer("radius").between(50, 1000);
        var yLevelArg = ArgumentType.Integer("yLevel").between(0, 255).setDefaultValue(64);

        addSyntax(this::execute, trackArg, radiusArg, yLevelArg);
        addSyntax(this::execute, trackArg, radiusArg);
    }

    private void execute(CommandSender sender, net.minestom.server.command.builder.CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return;
        }

        TrackName trackName = context.get("track");
        int radius = context.get("radius");
        int yLevel = context.get("yLevel") != null ? context.get("yLevel") : 64;

        player.sendMessage(Component.text("Scanning track edges for " + trackName + "...", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Center: " + player.getPosition().blockX() + ", " + player.getPosition().blockZ(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Radius: " + radius + ", Y-Level: " + yLevel, NamedTextColor.GRAY));
        player.sendMessage(Component.text("Looking for RED_CONCRETE (left) and BLUE_CONCRETE (right)", NamedTextColor.GRAY));

        instance.scheduler().scheduleNextTick(() -> {
            try {
                TrackEdgeScanner scanner = new TrackEdgeScanner(instance);

                TrackEdge[] edges = scanner.scanBothEdges(
                    player.getPosition().x(),
                    player.getPosition().z(),
                    radius,
                    yLevel,
                    Block.RED_CONCRETE,
                    Block.BLUE_CONCRETE
                );

                if (edges == null) {
                    player.sendMessage(Component.text("Failed to scan track edges! Make sure both RED_CONCRETE and BLUE_CONCRETE blocks are placed.", NamedTextColor.RED));
                    return;
                }

                TrackEdge leftEdge = edges[0];
                TrackEdge rightEdge = edges[1];

                TrackCenterline centerline = new TrackCenterline(leftEdge, rightEdge);

                TrackDataManager.saveTrackEdges(trackName, leftEdge, rightEdge);

                player.sendMessage(Component.text("✓ Track scan complete!", NamedTextColor.GREEN));
                player.sendMessage(Component.text("  Left edge: " + leftEdge.size() + " points (" + String.format("%.1f", leftEdge.getTotalLength()) + " blocks)", NamedTextColor.GRAY));
                player.sendMessage(Component.text("  Right edge: " + rightEdge.size() + " points (" + String.format("%.1f", rightEdge.getTotalLength()) + " blocks)", NamedTextColor.GRAY));
                player.sendMessage(Component.text("  Centerline: " + String.format("%.1f", centerline.getTotalLength()) + " blocks", NamedTextColor.GRAY));
                player.sendMessage(Component.text("Track data saved! You can now use this track for playback.", NamedTextColor.GREEN));

            } catch (Exception e) {
                player.sendMessage(Component.text("Error scanning track: " + e.getMessage(), NamedTextColor.RED));
                e.printStackTrace();
            }
        });
    }
}
