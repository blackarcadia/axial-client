package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HalloweenSkins;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tab-list heads use the same UUID assignment as the corresponding in-world player. */
@Mixin(PlayerListEntry.class)
public abstract class HalloweenPlayerListSkinMixin {
    @Inject(method="getSkinTextures",at=@At("RETURN"),cancellable=true)
    private void axial$halloweenListSkin(CallbackInfoReturnable<SkinTextures> cir) {
        var entry=(PlayerListEntry)(Object)this;
        cir.setReturnValue(HalloweenSkins.apply(entry.getProfile().id(),cir.getReturnValue()));
    }
}
