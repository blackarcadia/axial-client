package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PerformanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl",remap=false)
public abstract class PerformanceSodiumLiquidsMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$hideLiquids(CallbackInfo ci){if(!PerformanceConfig.liquids())ci.cancel();}
}
