package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmotePermissionState;
import io.github.kosmx.emotes.main.EmoteHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Saved native wheel entries cannot bypass a revoked VPS grant. */
@Mixin(value=EmoteHolder.class,remap=false)
public abstract class EmoteVpsOwnershipMixin {
    @Inject(method="playEmote()Z",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$requireOwnedEmote(CallbackInfoReturnable<Boolean> cir) {
        var self=(EmoteHolder)(Object)this;
        if(!EmotePermissionState.canUse(self))cir.setReturnValue(false);
    }
}
