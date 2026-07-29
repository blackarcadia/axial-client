package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenBackgroundMixin {
    private static final Identifier AXIAL_TITLE_BACKGROUND = AxialCosmetics.id("textures/gui/title/main_menu_background.png");
    private static final int AXIAL_TITLE_BACKGROUND_WIDTH = 1717;
    private static final int AXIAL_TITLE_BACKGROUND_HEIGHT = 916;
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private static final String OFFICIAL_GAMEMODES = "OFFICIAL GAMEMODES";
    private static final int OFFICIAL_GAMEMODES_RIGHT_MARGIN = 24;
    private static final int OFFICIAL_GAMEMODES_TOP = 126;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"
            )
    )
    private void axial_cosmetics$renderCustomBackground(TitleScreen screen, DrawContext context, float deltaTicks) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                AXIAL_TITLE_BACKGROUND,
                0,
                0,
                0.0f,
                0.0f,
                screen.width,
                screen.height,
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

    @Inject(method = "render", at = @At("TAIL"))
    private void axial_cosmetics$drawOfficialGamemodes(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        Text label = Text.literal(OFFICIAL_GAMEMODES).styled(style -> style.withFont(UI_FONT));
        int textWidth = textRenderer.getWidth(label);
        int x = Math.max(OFFICIAL_GAMEMODES_RIGHT_MARGIN, screen.width - OFFICIAL_GAMEMODES_RIGHT_MARGIN - textWidth);
        int y = OFFICIAL_GAMEMODES_TOP;

        drawOutlinedText(context, textRenderer, label, x, y, 0xFFFFFFFF, 0xFF000000);
    }

    private static void drawOutlinedText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int fillColor, int outlineColor) {
        context.drawText(textRenderer, text, x - 1, y, outlineColor, false);
        context.drawText(textRenderer, text, x + 1, y, outlineColor, false);
        context.drawText(textRenderer, text, x, y - 1, outlineColor, false);
        context.drawText(textRenderer, text, x, y + 1, outlineColor, false);
        context.drawText(textRenderer, text, x, y, fillColor, false);
    }
}
