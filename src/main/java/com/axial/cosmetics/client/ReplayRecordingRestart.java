package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.*;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.text.Text;

/** Restores an actionable start control when the previous recorder has been finalized. */
public final class ReplayRecordingRestart {
    private static volatile boolean requested;
    private ReplayRecordingRestart() {}
    public static boolean requested(){return requested;}
    public static void clear(){requested=false;}
    static boolean showStart(boolean enabled,boolean world,boolean playback,boolean active){return enabled&&world&&!playback&&!active;}
    public static boolean showStart() {
        var client=MinecraftClient.getInstance();boolean enabled=ReplayRecordingConfig.enabled(),world=client.world!=null;
        if(!enabled||!world)return showStart(enabled,world,false,false);
        try {
            boolean playback=NpcReplayBridge.playback()!=null;
            var recording=playback?null:NpcReplayBridge.recording();
            boolean active=recording!=null&&(!(recording.listener() instanceof ReplayRecorderStatus status)||!status.axial$isFinished());
            return showStart(enabled,world,playback,active);
        }catch(ReflectiveOperationException ex){return false;}
    }
    public static void open(Screen parent) {
        var client=MinecraftClient.getInstance();var server=client.getCurrentServerEntry();boolean singleplayer=client.isInSingleplayer();
        client.setScreen(new ConfirmScreen(accepted->{
            if(!accepted||!ReplayRecordingConfig.enabled()){client.setScreen(parent);return;}
            requested=true;
            client.disconnect(Text.literal("Starting a fresh replay recording"));
            if(!singleplayer&&server!=null)ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()),client,ServerAddress.parse(server.address),server,false,null);
            else client.setScreen(new SelectWorldScreen(new TitleScreen()));
        },ClientMenuText.literal("Start recording"),ClientMenuText.literal(singleplayer?
                "Save and reopen this world to start a fresh replay.":"Reconnect to this server to start a fresh replay. Your current connection will close."),
                ClientMenuText.literal(singleplayer?"Save & choose world":"Reconnect & record"),ClientMenuText.literal("Back")));
    }
    /** A manual start overrides native auto-record choices for this one connection only. */
    public static boolean forceSetting(Object key) {
        if(!requested)return false;
        try {var type=Class.forName("com.replaymod.recording.Setting");for(String name:new String[]{"RECORD_SINGLEPLAYER","RECORD_SERVER","AUTO_START_RECORDING"})if(type.getField(name).get(null)==key)return true;return false;}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Replay recording settings API changed",ex);}
    }
}
