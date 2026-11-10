package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Opens the standalone bundled mod without coupling compilation to its Loom version. */
final class LitematicaMenu {
    private LitematicaMenu() {}

    static void open(Screen parent) {
        try {
            var type = Class.forName("fi.dy.masa.litematica.gui.GuiMainMenu");
            var menu = type.getConstructor().newInstance();
            type.getMethod("setParent", Screen.class).invoke(menu, parent);
            MinecraftClient.getInstance().setScreen((Screen) menu);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not open the bundled Litematica menu", exception);
        }
    }
}
