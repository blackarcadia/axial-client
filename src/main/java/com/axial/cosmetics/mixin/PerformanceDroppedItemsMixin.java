package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PerformanceConfig;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderManager.class)
public abstract class PerformanceDroppedItemsMixin {
    // Cull before extracting item models, shadows or render commands.
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private void axial$hideDroppedItems(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> cir){
        if(!PerformanceConfig.droppedItems()&&entity instanceof ItemEntity)cir.setReturnValue(false);
    }
}
