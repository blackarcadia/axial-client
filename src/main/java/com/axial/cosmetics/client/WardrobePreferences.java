package com.axial.cosmetics.client;

import java.io.IOException;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** Per-installation cape visibility. Ownership remains bound to the creator UUID. */
final class WardrobePreferences {
    private static boolean loaded, showCape = true;
    private WardrobePreferences() { }

    static boolean showCape() {
        if (!loaded) {
            loaded = true;
            var path = FabricLoader.getInstance().getConfigDir().resolve("axial-wardrobe-cape.txt");
            try { if (Files.exists(path)) showCape = !Files.readString(path).trim().equals("hidden"); }
            catch (IOException ex) { org.slf4j.LoggerFactory.getLogger(WardrobePreferences.class).warn("Could not read wardrobe preference", ex); }
        }
        return showCape;
    }

    static void setShowCape(boolean value) throws IOException {
        if (!WardrobeAccess.allowed()) return;
        var path = FabricLoader.getInstance().getConfigDir().resolve("axial-wardrobe-cape.txt");
        Files.createDirectories(path.getParent());
        Files.writeString(path, value ? "visible\n" : "hidden\n");
        showCape = value;
        loaded = true;
    }
}
