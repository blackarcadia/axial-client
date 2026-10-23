package com.axial.cosmetics.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.axial.axialutils.client.LivingTransparencyState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** AxialUtils ships this state contract but no longer registers its implementing mixin. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateSmartMiningMixin implements LivingTransparencyState {
    @Unique private boolean axial$miningFade;
    @Override public boolean axialutils$shouldFade(){return axial$miningFade;}
    @Override public void axialutils$setShouldFade(boolean fade){axial$miningFade=fade;}
}
