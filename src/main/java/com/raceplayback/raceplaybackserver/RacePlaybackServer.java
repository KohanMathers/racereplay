package com.raceplayback.raceplaybackserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raceplayback.raceplaybackserver.data.Compound;
import com.raceplayback.raceplaybackserver.data.DataModelType;
import com.raceplayback.raceplaybackserver.data.SessionType;
import com.raceplayback.raceplaybackserver.data.TrackName;
import com.raceplayback.raceplaybackserver.network.F1ApiClient;

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.coordinate.Pos;

import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations;
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations;
import org.everbuild.blocksandstuff.blocks.PlacedHandlerRegistration;

public class RacePlaybackServer {

    private static RacePlaybackServer instance;

    private static final Logger logger = LoggerFactory.getLogger(RacePlaybackServer.class);

    public RacePlaybackServer() {
        instance = this;
    }

    public static RacePlaybackServer getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    public static void main(String[] args) {
        new RacePlaybackServer();
        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        instanceContainer.setChunkSupplier(LightingChunk::new);

        BlockPlacementRuleRegistrations.registerDefault();
        BlockBehaviorRuleRegistrations.registerDefault();
        PlacedHandlerRegistration.registerDefault();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 42, 0));
            player.setGameMode(GameMode.CREATIVE);
        });

        testApiClients();
        spawnTestObjects(instanceContainer);

        minecraftServer.start("0.0.0.0", 25565);
    }

    private static void testApiClients() {
        F1ApiClient client = new F1ApiClient("https://raceplayback.com/api/v1/sessions", 2024, TrackName.SILVERSTONE, SessionType.R, "drivers", DataModelType.NULL);
        if (client != null) {
            logger.info("F1 API Client is working!");
        }
    }

    private static void spawnTestObjects(InstanceContainer instance) {
        logger.info("Spawning test wheels...");
        
        int x = 0;
        int y = 0;
        for (Compound compound : Compound.values()) {
            var frontWheel = new Entity(EntityType.ITEM_DISPLAY);
            ItemStack frontItem = compound.createWheel(true);
            
            ItemDisplayMeta meta = (ItemDisplayMeta) frontWheel.getEntityMeta();
            meta.setItemStack(frontItem);
            meta.setHasNoGravity(true);
            
            frontWheel.setInstance(instance, new Pos(x, 42, y));

            if (y == 1) { y = 0; }        
            x += 1;
        }
        
        logger.info("All test wheels spawned!");

        logger.info("Spawning test steering wheel...");

        var steeringWheel = new Entity(EntityType.ITEM_DISPLAY);
        ItemStack steeringWheelItem = ItemStack.of(Material.STICK).withItemModel("raceplayback:steering_wheel");

        ItemDisplayMeta meta = (ItemDisplayMeta) steeringWheel.getEntityMeta();
        meta.setItemStack(steeringWheelItem);
        meta.setHasNoGravity(true);

        steeringWheel.setInstance(instance, new Pos(x, 42, y));
        if (y == 1) { y = 0; }
    }
}