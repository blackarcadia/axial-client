package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PerformanceConfig;
import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public abstract class PerformanceLiquidsMixin {
    @Inject(method="renderFluid",at=@At("HEAD"),cancellable=true)
    private void axial$hideLiquids(CallbackInfo ci){if(!PerformanceConfig.liquids())ci.cancel();}
}
