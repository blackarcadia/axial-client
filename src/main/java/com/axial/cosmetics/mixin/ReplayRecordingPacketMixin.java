package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayRecordingConfig;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.recording.packet.PacketListener",remap=false)
public abstract class ReplayRecordingPacketMixin implements com.axial.cosmetics.client.ReplayRecorderStatus {
    @Unique private final AtomicBoolean axial$finished=new AtomicBoolean();
    @Override public boolean axial$isFinished(){return axial$finished.get();}
    @Inject(method={"save(Lnet/minecraft/class_2596;)V","save(Lnet/minecraft/network/packet/Packet;)V"},at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$skipDisabledGamePacket(Packet<?> packet,CallbackInfo ci){if(axial$finished.get()||!ReplayRecordingConfig.enabled())ci.cancel();}
    @Inject(method="save(Lcom/replaymod/replaystudio/protocol/Packet;)V",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$skipDisabledReplayPacket(@Coerce Object packet,CallbackInfo ci){if(axial$finished.get()||!ReplayRecordingConfig.enabled()){ReplayRecordingConfig.discard(packet);ci.cancel();}}
    @Inject(method="channelInactive",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$finishOnlyOnce(ChannelHandlerContext context,CallbackInfo ci){if(!axial$finished.compareAndSet(false,true))ci.cancel();}
}
