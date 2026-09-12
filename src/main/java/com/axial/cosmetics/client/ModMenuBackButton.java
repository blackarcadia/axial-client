package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class ModMenuBackButton {
    private static final Identifier ICON = AxialCosmetics.id("textures/gui/back-arrow.png");

    private ModMenuBackButton() {
    }

    public static void draw(DrawContext context, int x, int y, int mouseX, int mouseY) {
        draw(context, x, y, 24, 18, mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 18);
    }

    public static void draw(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        context.fill(x, y, x + width, y + height, hovered ? 0xFF20283A : 0xFF14101F);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, hovered ? 0xFFE7D9FF : 0xFF8F5DFF);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON,
                x + (width - 16) / 2, y + (height - 13) / 2,
                0.0f, 0.0f, 16, 13, 64, 64, 64, 64);
    }
}
