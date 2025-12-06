package com.raceplayback.raceplaybackserver.entity.car.parts;

import com.raceplayback.raceplaybackserver.data.Compound;
import com.raceplayback.raceplaybackserver.entity.car.CarPart;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;

public class FrontWheel extends CarPart {
    private final Vec wheelScale = new Vec(0.54f, 0.66f, 0.66f);

    public FrontWheel(Vec offset, Compound compound) {
        super("temp", offset);

        ItemStack wheel = compound.createWheel(true);
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setItemStack(wheel);
    }

    @Override
    public void spawn(Instance instance, Pos carPosition, float yaw) {
        super.spawn(instance, carPosition, yaw);

        instance.scheduleNextTick(inst -> {
            setCustomScale(wheelScale);
            update(carPosition, yaw);
        });
    }
}