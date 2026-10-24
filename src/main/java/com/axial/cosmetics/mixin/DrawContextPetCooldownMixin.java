package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PetCooldownOverlay;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextPetCooldownMixin {
    @Inject(method="drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",at=@At("TAIL"))
    private void axial$petCooldown(TextRenderer font,ItemStack stack,int x,int y,String count,CallbackInfo ci){
        PetCooldownOverlay.render((DrawContext)(Object)this,stack,x,y);
    }
}
