package com.axial.cosmetics.client;

import com.axial.cosmetics.config.ReplayRecordingPreference;
import io.netty.channel.*;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ReplayRecordingConfig {
    private static volatile Boolean enabled;
    private static final java.util.Map<Object,ReplayRecorderStatus> buttons=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private ReplayRecordingConfig() {}
    private static Path file(){return FabricLoader.getInstance().getConfigDir().resolve("axial-replay-recording.json");}
    public static boolean enabled(){var value=enabled;if(value==null){synchronized(ReplayRecordingConfig.class){if(enabled==null)enabled=ReplayRecordingPreference.read(file());value=enabled;}}return value;}
    public static void toggle() {
        boolean next=!enabled();
        try {ReplayRecordingPreference.write(file(),next);enabled=next;if(!next){ReplayRecordingRestart.clear();stopCurrent();}}
        catch(Exception ex){throw new IllegalStateException("Could not update Replay Mod recording",ex);}
    }
    static String description(){return enabled()?"Recording on • Start Recording is available in the pause menu":"Recording off • Saved replays remain available";}
    private static void stopCurrent()throws Exception {
        var module=Class.forName("com.replaymod.recording.ReplayModRecording");var instance=module.getField("instance").get(null);if(instance==null)return;
        var connection=module.getMethod("getConnectionEventHandler").invoke(instance);if(connection==null)return;
        var listener=connection.getClass().getMethod("getPacketListener").invoke(connection);if(listener==null)return;
        var channelField=listener.getClass().getDeclaredField("channel");channelField.setAccessible(true);var channel=(Channel)channelField.get(listener);
        finish(channel,listener.getClass().getField("RAW_RECORDER_KEY").get(null).toString(),listener.getClass().getField("DECODED_RECORDER_KEY").get(null).toString(),()->{
            try{listener.getClass().getMethod("channelInactive",ChannelHandlerContext.class).invoke(listener,new Object[]{null});}
            catch(Exception ex){org.slf4j.LoggerFactory.getLogger(ReplayRecordingConfig.class).error("Could not finish the replay recording",ex);}
        });
    }
    /** Quiesce the network recorder before asking Replay Mod to flush and save. Never closes the connection. */
    static void finish(Channel channel,String raw,String decoded,Runnable save) {
        channel.eventLoop().execute(()->{
            var pipeline=channel.pipeline();if(pipeline.get(raw)!=null)pipeline.remove(raw);if(pipeline.get(decoded)!=null)pipeline.remove(decoded);save.run();
        });
    }
    public static boolean hideControl(Object button) {
        var recorder=buttons.get(button);return recorder!=null&&(!enabled()||recorder.axial$isFinished());
    }
    public static void registerControls(Object controls) {
        try {
            var field=controls.getClass().getDeclaredField("packetListener");field.setAccessible(true);var listener=(ReplayRecorderStatus)field.get(controls);
            for(String name:new String[]{"buttonStartStop","buttonPauseResume"}){var button=controls.getClass().getDeclaredField(name);button.setAccessible(true);buttons.put(button.get(controls),listener);}
        }catch(ReflectiveOperationException ex){throw new IllegalStateException("Replay recording controls API changed",ex);}
    }
    public static boolean controlsEnabled(Object controls) {
        if(!enabled())return false;
        try {var field=controls.getClass().getDeclaredField("packetListener");field.setAccessible(true);return !((ReplayRecorderStatus)field.get(controls)).axial$isFinished();}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Replay recording controls API changed",ex);}
    }
    public static void discard(Object packet) {
        try{packet.getClass().getMethod("release").invoke(packet);}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Could not release skipped replay packet",ex);}
    }
}
