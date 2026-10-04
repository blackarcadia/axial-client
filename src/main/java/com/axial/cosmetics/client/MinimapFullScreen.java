package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Full-screen, live view of the same terrain map used by the HUD minimap. */
public final class MinimapFullScreen extends Screen {
    public MinimapFullScreen() { super(Text.literal("Full Map")); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinimapRenderer.renderFullScreen(context, MinecraftClient.getInstance());
    }

    @Override public boolean shouldPause() { return false; }
}
