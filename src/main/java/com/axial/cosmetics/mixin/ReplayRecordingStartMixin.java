package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayRecordingConfig;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.recording.ReplayModRecording",remap=false)
public abstract class ReplayRecordingStartMixin {
    @Inject(method="initiateRecording",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$recordOnlyWhenEnabled(ClientConnection connection,CallbackInfo ci){if(!ReplayRecordingConfig.enabled())ci.cancel();}
}
