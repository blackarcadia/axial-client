package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import com.axial.cosmetics.client.EmoteMenuTheme;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.axial.cosmetics.client.EmoteWheelPreview;
import io.github.kosmx.emotes.arch.screen.widget.preview.elemets.PlayerChooseElement;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restyles Emotecraft's existing fast-menu emote tiles without changing their behaviour. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.preview.elemets.PlayerChooseElement", remap = false)
public abstract class EmoteRadialElementThemeMixin {
    private static final int SEGMENT_COUNT = 8;

    @Shadow protected AbstractFastChooseWidget parent;
    @Unique private boolean axial_cosmetics$iconHovered;
    @Unique private EmoteWheelPreview axial$preview;

    @Inject(method = "method_48579", at = @At("HEAD"), remap = false)
    private void axial_cosmetics$updateIconHover(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) {
            axial_cosmetics$iconHovered = false;
            return;
        }

        int size = Math.min(parent.getWidth(), parent.getHeight());
        int centerX = parent.getX() + parent.getWidth() / 2;
        int centerY = parent.getY() + parent.getHeight() / 2;
        int hoveredSegment = axial_cosmetics$segmentAt(centerX, centerY, Math.round(size * 0.47f * 0.34f),
                Math.round(size * 0.47f), mouseX, mouseY);
        int elementIndex = parent.getChooseElements().indexOf((IChooseElement) (Object) this);
        axial_cosmetics$iconHovered = elementIndex == hoveredSegment;
    }

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

        context.fill(x, y, x + width, y + height, EmoteMenuTheme.FIELD);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, EmoteMenuTheme.LINE);
        context.drawStrokedRectangle(x, y, width, height, EmoteMenuTheme.LINE);
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

        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, EmoteMenuTheme.HOVER);
        context.drawStrokedRectangle(x, y, width, height, EmoteMenuTheme.BRAND);
        ci.cancel();
    }

    /** Replace only icon/model content. Native background, tooltip and input code still runs. */
    @WrapOperation(method={"method_48579","renderWidget"},at={
            @At(value="INVOKE",target="Lnet/minecraft/class_332;method_25302(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/class_2960;IIFFIIIIII)V"),
            @At(value="INVOKE",target="Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIII)V")},remap=false)
    private void axial$animateIcon(DrawContext context,RenderPipeline pipeline,Identifier texture,int x,int y,float u,float v,int width,int height,int regionWidth,int regionHeight,int textureWidth,int textureHeight,Operation<Void> original) {
        if(!axial$drawPreview(context))original.call(context,pipeline,texture,x,y,u,v,width,height,regionWidth,regionHeight,textureWidth,textureHeight);
    }

    @WrapOperation(method={"method_48579","renderWidget"},at={
            @At(value="INVOKE",target="Lio/github/kosmx/emotes/arch/gui/widgets/PlayerPreview;method_48579(Lnet/minecraft/class_332;IIF)V"),
            @At(value="INVOKE",target="Lio/github/kosmx/emotes/arch/gui/widgets/PlayerPreview;renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V")},remap=false)
    private void axial$animateFallback(PlayerChooseElement preview,DrawContext context,int mouseX,int mouseY,float delta,Operation<Void> original) {
        if(!axial$drawPreview(context))original.call(preview,context,mouseX,mouseY,delta);
    }
    @Unique private boolean axial$drawPreview(DrawContext context) {
        var emote=((IChooseElement)(Object)this).getEmote();if(emote==null)return false;
        if(axial$preview==null)axial$preview=new EmoteWheelPreview();
        var tile=(ClickableWidget)(Object)this;
        axial$preview.draw(context,emote,tile.getX(),tile.getY(),tile.getWidth(),tile.getHeight(),
                parent.getX()+parent.getWidth()/2,parent.getY()+parent.getHeight()/2,
                Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get()),axial_cosmetics$iconHovered);
        return true;
    }

    @Unique
    private static int axial_cosmetics$segmentAt(int centerX, int centerY, int innerRadius, int outerRadius,
                                                  int mouseX, int mouseY) {
        double x = mouseX - centerX;
        double y = mouseY - centerY;
        double distance = Math.hypot(x, y);
        if (distance < innerRadius || distance > outerRadius) return -1;
        double segmentSize = Math.PI * 2 / SEGMENT_COUNT;
        return Math.floorMod((int) Math.floor((Math.PI / 2 - Math.atan2(y, x) + segmentSize / 2) / segmentSize),
                SEGMENT_COUNT);
    }
}
