package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PotionsHudConfig;
import com.axial.cosmetics.client.PotionsHudRenderer;
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
public abstract class AxialConfigScreenPotionsHudMoveMixin {
    @Unique private boolean axial_cosmetics$draggingPotionsHud;
    @Unique private int axial_cosmetics$potionsHudDragOffsetX;
    @Unique private int axial_cosmetics$potionsHudDragOffsetY;

    @Shadow private int snapPositionX(int desired, int elementSize, int maxValue) {
        throw new AssertionError();
    }

    @Shadow private int snapPositionY(int desired, int elementSize, int maxValue) {
        throw new AssertionError();
    }

    @Inject(method = "method_25394", at = @At("HEAD"))
    private void axial_cosmetics$renderPotionsHudPreview(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        PotionsHudRenderer.renderPreview(context, MinecraftClient.getInstance());
    }

    @Inject(method = "beginMoveDrag", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$beginPotionsHudDrag(MinecraftClient client, double mouseX, double mouseY,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!PotionsHudConfig.isEnabled()) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        var bounds = PotionsHudRenderer.bounds(MinecraftClient.getInstance(), true);
        int potionsHudWidth = bounds.width();
        int left = PotionsHudConfig.getX(screen.width, potionsHudWidth);
        int top = PotionsHudConfig.getY(screen.height, bounds.height());
        if (mouseX >= left && mouseX < left + potionsHudWidth
                && mouseY >= top && mouseY < top + bounds.height()) {
            axial_cosmetics$draggingPotionsHud = true;
            axial_cosmetics$potionsHudDragOffsetX = (int) mouseX - left;
            axial_cosmetics$potionsHudDragOffsetY = (int) mouseY - top;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25403", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$dragPotionsHud(Click click, double deltaX, double deltaY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingPotionsHud) {
            Screen screen = (Screen) (Object) this;
            var bounds = PotionsHudRenderer.bounds(MinecraftClient.getInstance(), true);
            int potionsHudWidth = bounds.width();
            int x = snapPositionX((int) click.x() - axial_cosmetics$potionsHudDragOffsetX,
                    potionsHudWidth, Math.max(0, screen.width - potionsHudWidth));
            int y = snapPositionY((int) click.y() - axial_cosmetics$potionsHudDragOffsetY,
                    bounds.height(), Math.max(0, screen.height - bounds.height()));
            PotionsHudConfig.setPosition(x, y);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25406", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$releasePotionsHud(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingPotionsHud && click.button() == 0) {
            axial_cosmetics$draggingPotionsHud = false;
            PotionsHudConfig.save();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = {"method_25404", "method_25402"}, at = @At("HEAD"))
    private void axial_cosmetics$savePotionsHudBeforeNavigation(CallbackInfoReturnable<Boolean> cir) {
        if (axial_cosmetics$draggingPotionsHud) {
            axial_cosmetics$draggingPotionsHud = false;
            PotionsHudConfig.save();
        }
    }
}
