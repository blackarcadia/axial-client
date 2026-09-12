package com.axial.cosmetics.client;

import org.axial.axialutils.client.AxialConfigManager;

public final class CompassConfig {
    private CompassConfig() {
    }

    public static boolean isEnabled() {
        Object config = AxialConfigManager.get();
        return config instanceof CompassConfigAccess access && Boolean.TRUE.equals(access.axial_cosmetics$getShowCompass());
    }

    public static void setEnabled(boolean enabled) {
        Object config = AxialConfigManager.get();
        if (config instanceof CompassConfigAccess access) {
            access.axial_cosmetics$setShowCompass(enabled);
        }
    }

    public static int getX(int screenWidth, int compassWidth) {
        Object config = AxialConfigManager.get();
        Integer x = config instanceof CompassConfigAccess access ? access.axial_cosmetics$getCompassX() : null;
        return Math.max(0, Math.min(x == null ? (screenWidth - compassWidth) / 2 : x, screenWidth - compassWidth));
    }

    public static int getY(int screenHeight, int compassHeight) {
        Object config = AxialConfigManager.get();
        Integer y = config instanceof CompassConfigAccess access ? access.axial_cosmetics$getCompassY() : null;
        return Math.max(0, Math.min(y == null ? 6 : y, screenHeight - compassHeight));
    }

    public static void setPosition(int x, int y) {
        Object config = AxialConfigManager.get();
        if (config instanceof CompassConfigAccess access) {
            access.axial_cosmetics$setCompassPosition(x, y);
        }
    }

    public static void toggle() {
        setEnabled(!isEnabled());
        AxialConfigManager.save();
    }

    public static void normalizeDefault() {
        Object config = AxialConfigManager.get();
        if (config instanceof CompassConfigAccess access && access.axial_cosmetics$getShowCompass() == null) {
            access.axial_cosmetics$setShowCompass(Boolean.TRUE);
        }
    }
}
