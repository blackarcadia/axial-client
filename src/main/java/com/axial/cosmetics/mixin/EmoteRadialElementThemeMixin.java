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
    private static final float ICON_SCALE = 0.60f;

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

    /** Fits Emotecraft's icon layer inside the inner medallions of the compact wheel texture. */
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
        int medallionX = x + Math.round((wheelCenterX - iconCenterX) * inward);
        int medallionY = y + Math.round((wheelCenterY - iconCenterY) * inward);
        int scaledWidth = Math.max(24, Math.round(width * ICON_SCALE));
        int scaledHeight = Math.max(24, Math.round(height * ICON_SCALE));

        // Scale around the preview's own centre after moving it, which keeps
        // every icon centred in its medallion regardless of aspect ratio.
        args.set(2, medallionX + (width - scaledWidth) / 2);
        args.set(3, medallionY + (height - scaledHeight) / 2);
        args.set(6, scaledWidth);
        args.set(7, scaledHeight);
    }
}
