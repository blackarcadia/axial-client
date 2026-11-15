package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Uses the supplied standalone Replay Mod's stable GUI entry point without remapping its JAR. */
public final class ReplayMenu {
    private ReplayMenu() {}
    public static void open(Screen parent) {
        try {
            var module=Class.forName("com.replaymod.replay.ReplayModReplay");
            var instance=module.getField("instance").get(null);
            if(instance==null)throw new IllegalStateException("Replay Mod has not initialized");
            var type=Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer");
            var viewer=type.getConstructor(module).newInstance(instance);
            if(parent!=null) {
                var cancel=type.getField("cancelButton").get(viewer);
                cancel.getClass().getMethod("onClick",Runnable.class).invoke(cancel,(Runnable)()->MinecraftClient.getInstance().setScreen(parent));
            }
            // toMinecraft is a Replay Mod method, so its name survives production remapping.
            var screen=(Screen)type.getMethod("toMinecraft").invoke(viewer);
            MinecraftClient.getInstance().setScreen(screen);
        }catch(ReflectiveOperationException exception){throw new IllegalStateException("Could not open the bundled Replay Mod viewer",exception);}
    }
}
