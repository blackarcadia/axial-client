package com.axial.cosmetics.data;

import java.util.Base64;

/**
 * Small 16x16 transparent placeholder texture used for the generated sample cosmetic.
 */
public final class PlaceholderTextures {
    private PlaceholderTextures() {}

    // 1x1 fully transparent PNG
    private static final String BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADUlEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg==";
    public static final byte[] HEADSET_PNG = Base64.getDecoder().decode(BASE64);
}
