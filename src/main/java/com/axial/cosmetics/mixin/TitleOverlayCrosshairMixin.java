package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairConfigManager;
import com.axial.cosmetics.client.CrosshairDynamicState;
import com.axial.cosmetics.client.CrosshairRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class TitleOverlayCrosshairMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void axial_cosmetics$hideCrosshairInTitleOverlay(DrawContext context,RenderTickCounter tickCounter,CallbackInfo ci){
        Screen screen=MinecraftClient.getInstance().currentScreen;
        if(screen!=null&&"org.axial.axialutils.client.TitleOverlaySettingsScreen".equals(screen.getClass().getName()))ci.cancel();
    }
    // Replace only the crosshair sprite, preserving vanilla spectator/debug rules and attack indicators.
    @WrapOperation(method="renderCrosshair",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V",ordinal=0))
    private void axial_cosmetics$renderCustomCrosshair(DrawContext context,RenderPipeline pipeline,Identifier sprite,int x,int y,int w,int h,Operation<Void> original){
        var config=CrosshairConfigManager.get();
        if(!config.enabled){original.call(context,pipeline,sprite,x,y,w,h);return;}
        var window=MinecraftClient.getInstance().getWindow();
        CrosshairRenderer.render(context,window.getScaledWidth()/2,window.getScaledHeight()/2,config,config.dynamicEnabled?CrosshairDynamicState.scaleMultiplier():1);
    }
}
