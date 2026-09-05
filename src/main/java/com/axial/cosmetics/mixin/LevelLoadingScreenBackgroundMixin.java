package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenBackgroundMixin {
    private static final Identifier AXIAL_LOADING_BACKGROUND = AxialCosmetics.id("textures/gui/title/sub_menu_background.png");
    private static final int AXIAL_LOADING_BACKGROUND_WIDTH = 1717;
    private static final int AXIAL_LOADING_BACKGROUND_HEIGHT = 916;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
            )
    )
    private void axial_cosmetics$renderLoadingBackground(Screen ignored, DrawContext context, int mouseX, int mouseY, float delta) {
        LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                AXIAL_LOADING_BACKGROUND,
                0,
                0,
                0.0f,
                0.0f,
                screen.width,
                screen.height,
                AXIAL_LOADING_BACKGROUND_WIDTH,
                AXIAL_LOADING_BACKGROUND_HEIGHT,
                AXIAL_LOADING_BACKGROUND_WIDTH,
                AXIAL_LOADING_BACKGROUND_HEIGHT
        );
    }
}
