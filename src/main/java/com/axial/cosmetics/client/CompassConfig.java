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
