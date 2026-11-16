package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayRecordingConfig;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.recording.handler.ConnectionEventHandler",remap=false)
public abstract class ReplayRecordingConnectionMixin {
    @Inject(method="onConnectedToServerEvent",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$blockDisabledConnection(ClientConnection connection,CallbackInfo ci){if(!ReplayRecordingConfig.enabled())ci.cancel();}
}
