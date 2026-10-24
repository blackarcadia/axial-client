package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class FpsHud {
    private FpsHud(){}
    public static void render(DrawContext context){
        var client=MinecraftClient.getInstance();
        if(!PerformanceConfig.displayFps()||client.player==null||client.options.hudHidden)return;
        context.drawText(client.textRenderer,client.getCurrentFps()+" FPS",8,8,PerformanceConfig.fpsColor(),true);
    }
}
