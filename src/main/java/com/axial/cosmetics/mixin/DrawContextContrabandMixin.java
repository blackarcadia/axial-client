package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.axial.axialutils.client.ContrabandHighlightState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** All GUI item entry points converge here, so each contraband overlay is drawn once. */
@Mixin(DrawContext.class)
public abstract class DrawContextContrabandMixin {
    @Inject(method="drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",at=@At("TAIL"))
    private void axial$highlightContraband(LivingEntity entity,World world,ItemStack stack,int x,int y,int seed,CallbackInfo ci) {
        int color=ContrabandHighlightState.getContrabandOverlayColor(stack);
        if(color!=0)((DrawContext)(Object)this).fill(x,y,x+16,y+16,color);
    }
}
