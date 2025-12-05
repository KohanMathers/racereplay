package com.raceplayback.raceplaybackserver.entity.car.parts;

import com.raceplayback.raceplaybackserver.entity.car.CarPart;
import net.minestom.server.coordinate.Vec;

public class CockpitLeft extends CarPart {
    public CockpitLeft() {
        super("cockpit_left", new Vec(0.3125, 0.5, 0));
    }
}