package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HalloweenSkins;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers local and remote players, including the model selection and first-person arm path. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class HalloweenPlayerSkinMixin {
    @Inject(method="getSkin",at=@At("RETURN"),cancellable=true)
    private void axial$halloweenSkin(CallbackInfoReturnable<SkinTextures> cir) {
        var player=(AbstractClientPlayerEntity)(Object)this;
        cir.setReturnValue(HalloweenSkins.apply(player.getUuid(),cir.getReturnValue()));
    }
}
