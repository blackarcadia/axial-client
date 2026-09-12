package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CompassConfig;
import com.axial.cosmetics.client.CompassHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenCompassMoveMixin {
    @Unique private boolean axial_cosmetics$draggingCompass;
    @Unique private int axial_cosmetics$compassDragOffsetX;
    @Unique private int axial_cosmetics$compassDragOffsetY;

    @Shadow private int snapPositionX(int desired, int elementSize, int maxValue) {
        throw new AssertionError();
    }

    @Shadow private int snapPositionY(int desired, int elementSize, int maxValue) {
        throw new AssertionError();
    }

    @Inject(method = "renderMoveMode", at = @At("RETURN"))
    private void axial_cosmetics$renderCompassPreview(DrawContext context, MinecraftClient client, CallbackInfo ci) {
        CompassHudRenderer.renderPreview(context, client);
    }

    @Inject(method = "beginMoveDrag", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$beginCompassDrag(MinecraftClient client, double mouseX, double mouseY,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!CompassConfig.isEnabled()) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        int compassWidth = CompassHudRenderer.getWidth(screen.width);
        int left = CompassConfig.getX(screen.width, compassWidth);
        int top = CompassConfig.getY(screen.height, CompassHudRenderer.HEIGHT);
        if (mouseX >= left && mouseX < left + compassWidth
                && mouseY >= top && mouseY < top + CompassHudRenderer.HEIGHT) {
            axial_cosmetics$draggingCompass = true;
            axial_cosmetics$compassDragOffsetX = (int) mouseX - left;
            axial_cosmetics$compassDragOffsetY = (int) mouseY - top;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25403", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$dragCompass(Click click, double deltaX, double deltaY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingCompass) {
            Screen screen = (Screen) (Object) this;
            int compassWidth = CompassHudRenderer.getWidth(screen.width);
            int x = snapPositionX((int) click.x() - axial_cosmetics$compassDragOffsetX,
                    compassWidth, Math.max(0, screen.width - compassWidth));
            int y = snapPositionY((int) click.y() - axial_cosmetics$compassDragOffsetY,
                    CompassHudRenderer.HEIGHT, Math.max(0, screen.height - CompassHudRenderer.HEIGHT));
            CompassConfig.setPosition(x, y);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25406", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$releaseCompass(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingCompass && click.button() == 0) {
            axial_cosmetics$draggingCompass = false;
            AxialConfigManager.save();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25404", at = @At("HEAD"))
    private void axial_cosmetics$saveCompassBeforeNavigation(CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingCompass) {
            AxialConfigManager.save();
        }
    }
}
