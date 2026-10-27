package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** One drag controller runs before the legacy handlers, so overlapping HUDs cannot move together. */
@Mixin(targets="org.axial.axialutils.client.AxialConfigScreen",remap=false,priority=1400)
public abstract class AxialConfigScreenHudEditorMixin implements HudEditorAccess {
    @Unique private final HudEditor axial$hudEditor=new HudEditor();
    @Unique private boolean axial$editingHud;
    @Override public boolean axial$isEditingHud(){return axial$editingHud;}

    @Inject(method="method_25394",at=@At("HEAD"))
    private void axial$resetEditorFrame(DrawContext context,int x,int y,float delta,CallbackInfo ci){axial$editingHud=false;}
    @Inject(method="renderMoveMode",at=@At("RETURN"))
    private void axial$previewXp(DrawContext context,MinecraftClient client,CallbackInfo ci){
        axial$editingHud=true;IslandXpPopup.preview(context,client);
    }
    @Inject(method="method_25394",at=@At("RETURN"))
    private void axial$drawEditorBorders(DrawContext context,int x,int y,float delta,CallbackInfo ci){
        if(axial$editingHud)axial$hudEditor.borders(context,MinecraftClient.getInstance(),x,y);
    }
    @Inject(method="beginMoveDrag",at=@At("HEAD"),cancellable=true)
    private void axial$beginHudDrag(MinecraftClient client,double x,double y,CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(axial$hudEditor.begin(client,x,y));
    }
    @Inject(method="method_25403",at=@At("HEAD"),cancellable=true)
    private void axial$dragHud(Click click,double dx,double dy,CallbackInfoReturnable<Boolean> cir){
        if(axial$hudEditor.drag(MinecraftClient.getInstance(),click.x(),click.y()))cir.setReturnValue(true);
    }
    @Inject(method="method_25406",at=@At("HEAD"),cancellable=true)
    private void axial$releaseHud(Click click,CallbackInfoReturnable<Boolean> cir){
        if(click.button()==0&&axial$hudEditor.finish())cir.setReturnValue(true);
    }
    @Inject(method={"method_25419","openMode","method_25426"},at=@At("HEAD"))
    private void axial$saveHudPosition(CallbackInfo ci){axial$hudEditor.finish();}
}
