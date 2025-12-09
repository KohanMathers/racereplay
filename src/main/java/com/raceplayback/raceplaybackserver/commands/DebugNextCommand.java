package com.raceplayback.raceplaybackserver.commands;

import com.raceplayback.raceplaybackserver.playback.DebugPlaybackController;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class DebugNextCommand extends Command {

    public DebugNextCommand() {
        super("debugnext");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use this command!");
                return;
            }

            DebugPlaybackController controller = SessionDebugCommand.getActiveController();

            if (controller == null) {
                player.sendMessage("§cNo active debug session! Use /sessiondebug first.");
                return;
            }

            controller.processNextPoint();
        });
    }
}
