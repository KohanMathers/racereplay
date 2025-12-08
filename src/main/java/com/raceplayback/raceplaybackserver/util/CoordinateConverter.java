package com.raceplayback.raceplaybackserver.util;

import net.minestom.server.coordinate.Pos;
import java.math.BigDecimal;

public class CoordinateConverter {
    private static final double SCALE = 1.0;
    
    private final double originX;
    private final double originZ;
    
    private Double firstTelemetryX = null;
    private Double firstTelemetryY = null;
    
    public CoordinateConverter(Pos trackOrigin) {
        this.originX = trackOrigin.x();
        this.originZ = trackOrigin.z();
    }

    public Pos toMinecraftPos(BigDecimal telemetryX, BigDecimal telemetryY, double height) {
        double telX = telemetryX.doubleValue();
        double telY = telemetryY.doubleValue();
        
        if (firstTelemetryX == null) {
            firstTelemetryX = telX;
            firstTelemetryY = telY;
        }
        
        double deltaX = (telX - firstTelemetryX) * SCALE;
        double deltaZ = (telY - firstTelemetryY) * SCALE;
        
        double mcX = originX + deltaX;
        double mcZ = originZ + deltaZ;
        
        return new Pos(mcX, height, mcZ);
    }

    public static float calculateYaw(Pos from, Pos to) {
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();

        if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
            return 0f;
        }

        double angle = Math.toDegrees(Math.atan2(dz, dx));

        float yaw = (float) (90 - angle);

        while (yaw > 180) yaw -= 360;
        while (yaw < -180) yaw += 360;

        return yaw;
    }
}