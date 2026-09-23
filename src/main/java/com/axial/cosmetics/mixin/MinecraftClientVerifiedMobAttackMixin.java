package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.VerifiedMobAttack;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientVerifiedMobAttackMixin {
    @Inject(method = "doAttack", at = @At("TAIL"))
    private void axial_cosmetics$startVerifiedMobAttack(CallbackInfoReturnable<Boolean> cir) {
        VerifiedMobAttack.recordManualAttack((MinecraftClient) (Object) this, cir.getReturnValue());
    }
}
