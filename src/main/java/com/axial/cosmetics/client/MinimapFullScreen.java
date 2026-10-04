package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/** Full-screen, live view of the same terrain map used by the HUD minimap. */
public final class MinimapFullScreen extends Screen {
    private float centerX;
    private float centerZ;
    private float blocksPerPixel = 1.0f;
    private boolean positioned;

    public MinimapFullScreen() { super(Text.literal("Full Map")); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!positioned && client.player != null) {
            centerX = client.player.getBlockX();
            centerZ = client.player.getBlockZ();
            blocksPerPixel = MinimapConfig.zoom();
            positioned = true;
        }
        MinimapRenderer.renderFullScreen(context, client, centerX, centerZ, blocksPerPixel);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public boolean keyPressed(KeyInput input) {
        if (AxialCosmetics.minimapMapKey().matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        blocksPerPixel = Math.max(0.25f, Math.min(4.0f, (float) (blocksPerPixel * (verticalAmount > 0 ? 0.8 : 1.25))));
        return true;
    }

    @Override public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        int size = Math.min(width - 48, height - 48);
        centerX -= (float) (deltaX * blocksPerPixel * 256 / size);
        centerZ -= (float) (deltaY * blocksPerPixel * 256 / size);
        return true;
    }
}
