package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.NpcEmoteDirector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import com.zigythebird.playeranimcore.animation.AnimationData;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only director-owned controllers receive an explicit clock; normal emotes remain untouched. */
@Pseudo
@Mixin(targets="com.zigythebird.playeranimcore.animation.AnimationController",remap=false)
public abstract class NpcEmoteClockMixin {
    @Shadow(remap=false) protected int tick;
    @Shadow(remap=false) protected float startAnimFrom;
    // process() reads these fields directly instead of calling getAnimationTicks().
    @Inject(method="process",at=@At("HEAD"),remap=false)
    private void axial$sampleActorPose(AnimationData data,CallbackInfo ci) {
        var time=NpcEmoteDirector.overrideTicks(this);
        if(time!=null){tick=0;startAnimFrom=time-data.getPartialTick();}
    }
    @Inject(method="getAnimationTicks",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$actorReplayTime(CallbackInfoReturnable<Float> cir) {
        var tick=NpcEmoteDirector.overrideTicks(this);if(tick!=null)cir.setReturnValue(tick);
    }
}
