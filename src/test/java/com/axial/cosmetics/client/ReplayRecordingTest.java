package com.axial.cosmetics.client;

import com.axial.cosmetics.config.ReplayRecordingPreference;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReplayRecordingTest {
    @TempDir Path directory;
    @Test void preferencePersistsBothWaysAndMalformedConfigFailsClosed()throws Exception {
        var file=directory.resolve("config/replay.json");assertTrue(ReplayRecordingPreference.read(file));
        ReplayRecordingPreference.write(file,false);assertFalse(ReplayRecordingPreference.read(file));
        ReplayRecordingPreference.write(file,true);assertTrue(ReplayRecordingPreference.read(file));
        Files.writeString(file,"broken json");assertFalse(ReplayRecordingPreference.read(file));
        Files.writeString(file,"{}");assertFalse(ReplayRecordingPreference.read(file));
        try(var files=Files.list(file.getParent())){assertEquals(1,files.count());}
    }
    @Test void disablingDetachesOnlyReplayHandlersFlushesAndKeepsTheGameConnectionAlive() {
        var saved=new AtomicInteger();var game=new ChannelInboundHandlerAdapter();
        var channel=new EmbeddedChannel();try {
            channel.pipeline().addLast("raw",new ChannelInboundHandlerAdapter());channel.pipeline().addLast("decoded",new ChannelInboundHandlerAdapter());channel.pipeline().addLast("game",game);
            ReplayRecordingConfig.finish(channel,"raw","decoded",()->{
                assertNull(channel.pipeline().get("raw"));assertNull(channel.pipeline().get("decoded"));assertSame(game,channel.pipeline().get("game"));saved.incrementAndGet();
            });
            channel.runPendingTasks();assertEquals(1,saved.get());assertTrue(channel.isActive());
            assertTrue(channel.writeInbound("still connected"));assertEquals("still connected",channel.readInbound());
        }finally{channel.finishAndReleaseAll();}
    }
    static final class RecorderHooks extends com.axial.cosmetics.mixin.ReplayRecordingPacketMixin {}
    @Test void manualStopThenDisconnectFinalizesOnlyOnce()throws Exception {
        var hooks=new RecorderHooks();var method=com.axial.cosmetics.mixin.ReplayRecordingPacketMixin.class.getDeclaredMethod("axial$finishOnlyOnce",ChannelHandlerContext.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
        var first=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("finish",true);method.invoke(hooks,null,first);assertFalse(first.isCancelled());
        var second=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("disconnect",true);method.invoke(hooks,null,second);assertTrue(second.isCancelled());
    }
    @Test void recordingOffDropsPacketsAndClosedRecordersStayClosedAfterReEnabling()throws Exception {
        var setting=ReplayRecordingConfig.class.getDeclaredField("enabled");setting.setAccessible(true);var old=setting.get(null);
        try {
            setting.set(null,false);var hooks=new RecorderHooks();
            var save=com.axial.cosmetics.mixin.ReplayRecordingPacketMixin.class.getDeclaredMethod("axial$skipDisabledReplayPacket",Object.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);save.setAccessible(true);
            var packet=new Packet();var callback=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("save",true);save.invoke(hooks,packet,callback);assertTrue(callback.isCancelled());assertEquals(0,packet.references);
            var finish=com.axial.cosmetics.mixin.ReplayRecordingPacketMixin.class.getDeclaredMethod("axial$finishOnlyOnce",ChannelHandlerContext.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);finish.setAccessible(true);
            finish.invoke(hooks,null,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("finish",true));setting.set(null,true);
            var stale=new Packet();var late=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("late save",true);save.invoke(hooks,stale,late);assertTrue(late.isCancelled());assertEquals(0,stale.references);
        }finally{setting.set(null,old);}
    }
    public static final class Controls {
        private final ReplayRecorderStatus packetListener;
        private final Object buttonStartStop=new Object(),buttonPauseResume=new Object();
        Controls(ReplayRecorderStatus status){packetListener=status;}
    }
    @Test void cachedPauseScreenControlsHideWhileOffAndNeverReactivateAClosedRecording()throws Exception {
        var setting=ReplayRecordingConfig.class.getDeclaredField("enabled");setting.setAccessible(true);var old=setting.get(null);
        try {
            var finished=new AtomicBoolean();var controls=new Controls(finished::get);ReplayRecordingConfig.registerControls(controls);
            setting.set(null,true);assertFalse(ReplayRecordingConfig.hideControl(controls.buttonStartStop));assertTrue(ReplayRecordingConfig.controlsEnabled(controls));
            setting.set(null,false);assertTrue(ReplayRecordingConfig.hideControl(controls.buttonStartStop));assertTrue(ReplayRecordingConfig.hideControl(controls.buttonPauseResume));assertFalse(ReplayRecordingConfig.controlsEnabled(controls));
            finished.set(true);setting.set(null,true);assertTrue(ReplayRecordingConfig.hideControl(controls.buttonStartStop));assertFalse(ReplayRecordingConfig.controlsEnabled(controls));
        }finally{setting.set(null,old);}
    }
    public static final class Packet {int references=1;public boolean release(){references--;return true;}}
    @Test void skippedReplayPacketReleasesItsOwnedBufferReference() {
        var packet=new Packet();ReplayRecordingConfig.discard(packet);assertEquals(0,packet.references);
    }
}
