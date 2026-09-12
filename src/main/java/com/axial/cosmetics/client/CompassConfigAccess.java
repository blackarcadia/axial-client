package com.axial.cosmetics.client;

public interface CompassConfigAccess {
    Boolean axial_cosmetics$getShowCompass();

    void axial_cosmetics$setShowCompass(Boolean showCompass);
    Integer axial_cosmetics$getCompassX();

    Integer axial_cosmetics$getCompassY();

    void axial_cosmetics$setCompassPosition(int x, int y);
}
