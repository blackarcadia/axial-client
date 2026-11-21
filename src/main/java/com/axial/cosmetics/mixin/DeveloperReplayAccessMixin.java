package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.DeveloperAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.replaymod.replay.ReplayModReplay",remap=false)
public abstract class DeveloperReplayAccessMixin {
    @Inject(method={"startReplay(Ljava/io/File;)V","startReplay(Lcom/replaymod/replaystudio/replay/ReplayFile;)V"},at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$blockPlayback(CallbackInfo ci){if(!DeveloperAccess.allowed())ci.cancel();}
    @Inject(method="startReplay(Lcom/replaymod/replaystudio/replay/ReplayFile;ZZ)Lcom/replaymod/replay/ReplayHandler;",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$blockDirectPlayback(CallbackInfoReturnable<Object> cir){if(!DeveloperAccess.allowed())cir.setReturnValue(null);}
}
