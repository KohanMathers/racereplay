package com.raceplayback.raceplaybackserver.commands;

import com.raceplayback.raceplaybackserver.data.TrackName;
import com.raceplayback.raceplaybackserver.mapping.TrackCenterline;
import com.raceplayback.raceplaybackserver.mapping.TrackDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.ArrayList;
import java.util.List;

public class VisualizeCenterlineCommand extends Command {
    private final Instance instance;
    private static final List<Entity> visualizationEntities = new ArrayList<>();

    public VisualizeCenterlineCommand(Instance instance) {
        super("visualizecenterline", "showcenterline");
        this.instance = instance;

        var trackArg = ArgumentType.Enum("track", TrackName.class);

        addSyntax(this::executeVisualize, trackArg);
        addSyntax(this::executeClear, ArgumentType.Literal("clear"));
    }

    private void executeVisualize(CommandSender sender, net.minestom.server.command.builder.CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return;
        }

        TrackName trackName = context.get("track");

        player.sendMessage(Component.text("Loading centerline for " + trackName + "...", NamedTextColor.YELLOW));

        TrackCenterline centerline = TrackDataManager.loadTrackCenterline(trackName);

        if (centerline == null) {
            player.sendMessage(Component.text("No track data found for " + trackName, NamedTextColor.RED));
            player.sendMessage(Component.text("Run /scantrack first!", NamedTextColor.RED));
            return;
        }

        clearVisualization();

        player.sendMessage(Component.text("Visualizing centerline...", NamedTextColor.YELLOW));

        int samples = 500;
        for (int i = 0; i <= samples; i++) {
            double percent = i / (double) samples;
            Pos pos = centerline.getPositionAtPercent(percent);

            Entity blockDisplay = new Entity(EntityType.BLOCK_DISPLAY);
            BlockDisplayMeta meta = (BlockDisplayMeta) blockDisplay.getEntityMeta();
            meta.setBlockState(Block.EMERALD_BLOCK);
            meta.setScale(new Vec(0.3, 0.3, 0.3));
            meta.setHasNoGravity(true);

            blockDisplay.setInstance(instance, pos.add(0, 1, 0));
            visualizationEntities.add(blockDisplay);
        }

        player.sendMessage(Component.text("✓ Centerline visualization complete!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("  Total length: " + String.format("%.1f", centerline.getTotalLength()) + " blocks", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Emerald blocks = centerline", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Use /visualizecenterline clear to remove", NamedTextColor.GRAY));
    }

    private void executeClear(CommandSender sender, net.minestom.server.command.builder.CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return;
        }

        clearVisualization();
        player.sendMessage(Component.text("Cleared centerline visualization", NamedTextColor.GREEN));
    }

    private void clearVisualization() {
        for (Entity entity : visualizationEntities) {
            entity.remove();
        }
        visualizationEntities.clear();
    }
}
