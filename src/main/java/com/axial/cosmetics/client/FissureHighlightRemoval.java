package com.axial.cosmetics.client;

import org.axial.axialutils.client.AxialConfigManager;

public final class FissureHighlightRemoval {
    private FissureHighlightRemoval() {
    }

    public static void disable() {
        AxialConfigManager.get().showFissureHighlights = Boolean.FALSE;
    }
}
