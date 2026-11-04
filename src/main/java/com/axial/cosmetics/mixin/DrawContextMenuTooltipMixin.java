package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.MenuTooltipRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMenuTooltipMixin {
    @Inject(method = "drawTooltipImmediately", at = @At("HEAD"), cancellable = true)
    private void axial$menuTooltip(TextRenderer font, List<TooltipComponent> components, int x, int y,
                                   TooltipPositioner positioner, Identifier texture, CallbackInfo ci) {
        if (MenuTooltipRenderer.render((DrawContext) (Object) this, font, components, x, y, positioner, texture)) {
            ci.cancel();
        }
    }
}
