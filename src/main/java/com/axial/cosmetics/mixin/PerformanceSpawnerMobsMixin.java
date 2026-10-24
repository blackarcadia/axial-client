package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PerformanceConfig;
import net.minecraft.client.render.block.entity.TrialSpawnerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrialSpawnerBlockEntityRenderer.class)
public abstract class PerformanceSpawnerMobsMixin {
    // Both regular and trial spawners use this helper to prepare their miniature mob.
    @Inject(method="updateSpawnerRenderState",at=@At("HEAD"),cancellable=true)
    private static void axial$hideSpawnerMob(MobSpawnerBlockEntityRenderState state,float delta,Entity entity,EntityRenderManager manager,double previous,double current,CallbackInfo ci){
        if(PerformanceConfig.disableSpawnerMobs()){state.displayEntityRenderState=null;ci.cancel();}
    }
}
