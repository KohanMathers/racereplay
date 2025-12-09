package com.raceplayback.raceplaybackserver.entity.car;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public abstract class CarPart {
    protected Entity entity;
    protected Vec offset;
    protected Vec baseOffset;
    protected float rotationOffset = 0;
    protected Vec currentScale;

    private static final float SCALE = 1.0f;

    public CarPart(String modelName, Vec offset) {
        this.offset = offset;
        this.baseOffset = offset;
        this.currentScale = new Vec(SCALE, SCALE, SCALE);
        this.entity = new Entity(EntityType.ITEM_DISPLAY);

        ItemStack model = ItemStack.of(Material.STICK)
            .withItemModel("raceplayback:" + modelName);

        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setItemStack(model);
        meta.setHasNoGravity(true);

        meta.setScale(currentScale);
    }
    
    public void spawn(Instance instance, Pos carPosition, float yaw) {
        Pos partPosition = calculatePosition(carPosition, yaw);
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setRightRotation(createYawRotation(yaw + rotationOffset));
        entity.setInstance(instance, partPosition);
    }

    public void update(Pos carPosition, float yaw) {
        Pos partPosition = calculatePosition(carPosition, yaw);

        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setRightRotation(createYawRotation(yaw + rotationOffset));

        entity.teleport(partPosition.withYaw(0));

        entity.sendPacketToViewers(entity.getMetadataPacket());
    }

    protected void updateRotation(float yaw) {
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setRightRotation(createYawRotation(yaw + rotationOffset));
    }

    private float[] createYawRotation(float yaw) {
        float rad = (float) Math.toRadians(yaw);
        float halfAngle = rad / 2.0f;
        float sin = (float) Math.sin(halfAngle);
        float cos = (float) Math.cos(halfAngle);

        return new float[] {0.0f, sin, 0.0f, cos};
    }
    
    public void remove() {
        entity.remove();
    }
    
    public void setVisible(boolean visible) {
        entity.setInvisible(!visible);
    }
    
    protected Pos calculatePosition(Pos carPosition, float yaw) {
        Vec rotated = rotateOffset(offset, yaw);
        return carPosition.add(rotated);
    }

    protected Vec rotateOffset(Vec offset, float yaw) {
        double rad = Math.toRadians(-yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        return new Vec(
            offset.x() * cos - offset.z() * sin,
            offset.y(),
            offset.x() * sin + offset.z() * cos
        );
    }
    
    public Entity getEntity() {
        return entity;
    }

    public float[] getLeftRotation() {
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        return meta.getLeftRotation();
    }

    public float[] getRightRotation() {
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        return meta.getRightRotation();
    }

    public float getYawFromRotation() {
        float[] quat = getRightRotation();
        float yaw = (float) (2.0 * Math.atan2(quat[1], quat[3]));
        return (float) Math.toDegrees(yaw);
    }
    
    protected void setCustomScale(Vec scale) {
        this.currentScale = scale;
        this.offset = new Vec(
            baseOffset.x() * scale.x(),
            baseOffset.y() * scale.y(),
            baseOffset.z() * scale.z()
        );
        ItemDisplayMeta meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setScale(scale);
    }
}