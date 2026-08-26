package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProgressScreen.class)
public abstract class ProgressScreenBackgroundMixin {
    private static final Identifier AXIAL_SAVE_BACKGROUND = AxialCosmetics.id("textures/gui/title/main_menu_background.png");
    private static final Identifier AXIAL_LOADING_BACKGROUND = AxialCosmetics.id("textures/gui/title/sub_menu_background.png");
    private static final int AXIAL_BACKGROUND_WIDTH = 1717;
    private static final int AXIAL_BACKGROUND_HEIGHT = 916;

    @Shadow
    private Text title;

    @Shadow
    private Text task;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
            )
    )
    private void axial_cosmetics$renderProgressBackground(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier background = axial_cosmetics$shouldUseSaveBackground()
                ? AXIAL_SAVE_BACKGROUND
                : AXIAL_LOADING_BACKGROUND;
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                background,
                0,
                0,
                0.0f,
                0.0f,
                screen.width,
                screen.height,
                AXIAL_BACKGROUND_WIDTH,
                AXIAL_BACKGROUND_HEIGHT,
                AXIAL_BACKGROUND_WIDTH,
                AXIAL_BACKGROUND_HEIGHT
        );
    }

    private boolean axial_cosmetics$shouldUseSaveBackground() {
        String combined = "";
        if (this.title != null) {
            combined += this.title.getString();
        }
        if (this.task != null) {
            combined += " " + this.task.getString();
        }

        String lower = combined.toLowerCase();
        return lower.contains("saving") || lower.contains("save and quit") || lower.contains("saved world");
    }
}
