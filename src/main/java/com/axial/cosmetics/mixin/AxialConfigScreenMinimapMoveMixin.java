package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.MinimapConfig;
import com.axial.cosmetics.client.MinimapRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenMinimapMoveMixin {
    @Unique private boolean axial_cosmetics$draggingMinimap;
    @Unique private int axial_cosmetics$minimapDragOffsetX;
    @Unique private int axial_cosmetics$minimapDragOffsetY;
    @Shadow private int snapPositionX(int desired, int elementSize, int maxValue) { throw new AssertionError(); }
    @Shadow private int snapPositionY(int desired, int elementSize, int maxValue) { throw new AssertionError(); }

    @Inject(method = "renderMoveMode", at = @At("RETURN"))
    private void axial_cosmetics$renderMinimapPreview(DrawContext context, MinecraftClient client, CallbackInfo ci) { MinimapRenderer.renderPreview(context, client); }

    @Inject(method = "beginMoveDrag", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$beginMinimapDrag(MinecraftClient client, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!MinimapConfig.isEnabled()) return;
        Screen screen = (Screen) (Object) this;
        int left = MinimapConfig.getX(screen.width), top = MinimapConfig.getY(screen.height);
        if (mouseX >= left && mouseX < left + MinimapRenderer.SIZE && mouseY >= top && mouseY < top + MinimapRenderer.SIZE) {
            axial_cosmetics$draggingMinimap = true;
            axial_cosmetics$minimapDragOffsetX = (int) mouseX - left;
            axial_cosmetics$minimapDragOffsetY = (int) mouseY - top;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25403", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$dragMinimap(Click click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!axial_cosmetics$draggingMinimap) return;
        Screen screen = (Screen) (Object) this;
        MinimapConfig.setPosition(snapPositionX((int) click.x() - axial_cosmetics$minimapDragOffsetX, MinimapRenderer.SIZE, Math.max(0, screen.width - MinimapRenderer.SIZE)), snapPositionY((int) click.y() - axial_cosmetics$minimapDragOffsetY, MinimapRenderer.SIZE, Math.max(0, screen.height - MinimapRenderer.SIZE)));
        cir.setReturnValue(true);
    }

    @Inject(method = "method_25406", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$releaseMinimap(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingMinimap && click.button() == 0) { axial_cosmetics$draggingMinimap = false; MinimapConfig.save(); cir.setReturnValue(true); }
    }
}
