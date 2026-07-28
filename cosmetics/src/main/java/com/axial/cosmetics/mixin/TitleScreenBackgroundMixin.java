package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public abstract class TitleScreenBackgroundMixin {
    private static final Identifier AXIAL_TITLE_BACKGROUND = AxialCosmetics.id("textures/gui/title/main_menu_background.png");
    private static final int AXIAL_TITLE_BACKGROUND_WIDTH = 1717;
    private static final int AXIAL_TITLE_BACKGROUND_HEIGHT = 916;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"
            )
    )
    private void axial_cosmetics$renderCustomBackground(TitleScreen screen, DrawContext context, float deltaTicks) {
        int x = Math.max(0, (screen.width - AXIAL_TITLE_BACKGROUND_WIDTH) / 2);
        int y = Math.max(0, (screen.height - AXIAL_TITLE_BACKGROUND_HEIGHT) / 2);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                AXIAL_TITLE_BACKGROUND,
                x,
                y,
                0.0f,
                0.0f,
                AXIAL_TITLE_BACKGROUND_WIDTH,
                AXIAL_TITLE_BACKGROUND_HEIGHT,
                AXIAL_TITLE_BACKGROUND_WIDTH,
                AXIAL_TITLE_BACKGROUND_HEIGHT
        );
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
            )
    )
    private void axial_cosmetics$skipLogo(LogoDrawer logoDrawer, DrawContext context, int width, float alpha) {
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;F)V"
            )
    )
    private void axial_cosmetics$skipSplash(SplashTextRenderer splashTextRenderer, DrawContext context, int width, TextRenderer textRenderer, float alpha) {
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
            )
    )
    private void axial_cosmetics$moveCopyrightText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        if (text.contains("Copyright Mojang AB") || text.contains("Fabric") || text.contains("Mods")) {
            return;
        }
        TitleScreen screen = (TitleScreen) (Object) this;
        int rightX = Math.max(2, screen.width - textRenderer.getWidth(text) - 2);
        context.drawTextWithShadow(textRenderer, text, rightX, y, color);
    }
}
