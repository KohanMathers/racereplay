package com.raceplayback.raceplaybackserver.data;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public enum Compound {
    SOFT("soft"),
    MEDIUM("medium"),
    HARD("hard"),
    INTERMEDIATE("intermediate"),
    WET("wet");
    
    private final String modelName;
    
    Compound(String modelName) {
        this.modelName = modelName;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public ItemStack createWheel(boolean isFront) {
        String position = isFront ? "front" : "rear";
        String fullModelName = "raceplayback:" + position + "_wheel_" + modelName;
        
        return ItemStack.of(Material.STICK).withItemModel(fullModelName);
    }
}