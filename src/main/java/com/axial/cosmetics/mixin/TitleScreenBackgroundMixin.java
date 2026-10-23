package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.TitleMenuAccess;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the approved menu, including the bulletins, without the obsolete fixed hit boxes. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenBackgroundMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void axial_cosmetics$renderTitleMenu(DrawContext context,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        var menu=((TitleMenuAccess)this).axial$titleMenu();
        if(menu==null)return;
        menu.render(context,mouseX,mouseY,delta);
        ci.cancel();
    }
}
