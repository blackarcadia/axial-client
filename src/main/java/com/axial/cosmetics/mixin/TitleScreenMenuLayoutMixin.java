package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.TitleMenu;
import com.axial.cosmetics.client.TitleMenuAccess;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains TitleScreen's lifecycle, with one responsive set of native controls. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMenuLayoutMixin extends Screen implements TitleMenuAccess {
    @Unique private TitleMenu axial_cosmetics$menu;
    protected TitleScreenMenuLayoutMixin(Text title){super(title);}
    @Override public TitleMenu axial$titleMenu(){return axial_cosmetics$menu;}

    @Inject(method="init",at=@At("TAIL"))
    private void axial_cosmetics$buildTitleMenu(CallbackInfo ci) {
        clearChildren();
        if(axial_cosmetics$menu==null)axial_cosmetics$menu=new TitleMenu((TitleScreen)(Object)this);
        axial_cosmetics$menu.init(widget->addDrawableChild(widget));
    }
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void axial_cosmetics$clickVisibleControls(Click click,boolean doubled,CallbackInfoReturnable<Boolean> cir) {
        if(axial_cosmetics$menu!=null)cir.setReturnValue(super.mouseClicked(click,doubled));
    }
}
