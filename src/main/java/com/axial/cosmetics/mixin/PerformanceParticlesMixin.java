package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PerformanceConfig;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.particle.Particle;

@Mixin(ParticleManager.class)
public abstract class PerformanceParticlesMixin {
    @Inject(method="addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",at=@At("HEAD"),cancellable=true)
    private void axial$skipParticleCreation(CallbackInfoReturnable<Particle> cir){if(!PerformanceConfig.particles())cir.setReturnValue(null);}
    @Inject(method="addParticle(Lnet/minecraft/client/particle/Particle;)V",at=@At("HEAD"),cancellable=true)
    private void axial$skipParticleQueue(CallbackInfo ci){if(!PerformanceConfig.particles())ci.cancel();}
    @Inject(method="addToBatch",at=@At("HEAD"),cancellable=true)
    private void axial$hideParticles(CallbackInfo ci){if(!PerformanceConfig.particles())ci.cancel();}
}
