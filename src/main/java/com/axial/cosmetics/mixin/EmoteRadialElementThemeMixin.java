package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restyles Emotecraft's existing fast-menu emote tiles without changing their behaviour. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.preview.elemets.PlayerChooseElement", remap = false)
public abstract class EmoteRadialElementThemeMixin {
    @Shadow protected AbstractFastChooseWidget parent;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$drawAxialTile(DrawContext context, CallbackInfo ci) {
        // The legacy layout is rendered as one unified wheel by
        // EmoteRadialWheelThemeMixin. Keep these individual tiles transparent so
        // their existing emote previews sit naturally inside the segments.
        if (Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) {
            ci.cancel();
            return;
        }
        ClickableWidget tile = (ClickableWidget) (Object) this;
        int x = tile.getX();
        int y = tile.getY();
        int width = tile.getWidth();
        int height = tile.getHeight();

        context.fill(x, y, x + width, y + height, 0xD8101018);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, 0xD08F5DFF);
        ci.cancel();
    }

    @Inject(method = "renderHover", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$drawAxialHover(DrawContext context, CallbackInfo ci) {
        if (Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) {
            ci.cancel();
            return;
        }
        ClickableWidget tile = (ClickableWidget) (Object) this;
        int x = tile.getX();
        int y = tile.getY();
        int width = tile.getWidth();
        int height = tile.getHeight();

        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x6A7440A8);
        context.drawStrokedRectangle(x, y, width, height, 0xFFB06AF3);
        ci.cancel();
    }

    /** Aligns Emotecraft's icon layer with the inner medallions in the compact wheel texture. */
    @ModifyArgs(
            method = "method_48579",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25302(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/class_2960;IIFFIIIIII)V"),
            remap = false
    )
    private void axial_cosmetics$centerIconInMedallion(Args args) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) return;

        int x = args.get(2);
        int y = args.get(3);
        int width = args.get(6);
        int height = args.get(7);
        int wheelCenterX = parent.getX() + parent.getWidth() / 2;
        int wheelCenterY = parent.getY() + parent.getHeight() / 2;
        int iconCenterX = x + width / 2;
        int iconCenterY = y + height / 2;

        // The preview elements sit on Emotecraft's wider legacy orbit. Pull
        // them toward the center to match the texture's medallion orbit.
        float inward = 0.28f;
        args.set(2, x + Math.round((wheelCenterX - iconCenterX) * inward));
        args.set(3, y + Math.round((wheelCenterY - iconCenterY) * inward));
    }
}
