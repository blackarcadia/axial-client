package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

final class MenuBackgroundRenderer {
    static final Identifier BACKGROUND = AxialCosmetics.id("textures/gui/title/sub_menu_background.png");
    static final int BACKGROUND_WIDTH = 1717;
    static final int BACKGROUND_HEIGHT = 916;

    private MenuBackgroundRenderer() {
    }

    static void draw(DrawContext context, Screen screen) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                0,
                0,
                0.0f,
                0.0f,
                screen.width,
                screen.height,
                BACKGROUND_WIDTH,
                BACKGROUND_HEIGHT,
                BACKGROUND_WIDTH,
                BACKGROUND_HEIGHT
        );
    }
}
