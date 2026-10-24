package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CombatCooldownHud;
import com.axial.cosmetics.client.CooldownsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="org.axial.axialutils.client.AxialConfigScreen",remap=false,priority=1100)
public abstract class AxialConfigScreenCooldownsMoveMixin {
    @Unique private boolean axial$draggingCooldowns;
    @Unique private int axial$cooldownsOffsetX,axial$cooldownsOffsetY;
    @Shadow private int snapPositionX(int desired,int size,int max){throw new AssertionError();}
    @Shadow private int snapPositionY(int desired,int size,int max){throw new AssertionError();}
    @Inject(method="renderMoveMode",at=@At("RETURN"))
    private void axial$previewCooldowns(DrawContext context,MinecraftClient client,CallbackInfo ci){CombatCooldownHud.preview(context,client);}
    @Inject(method="beginMoveDrag",at=@At("HEAD"),cancellable=true)
    private void axial$beginCooldownsDrag(MinecraftClient client,double x,double y,CallbackInfoReturnable<Boolean> cir){
        var bounds=CombatCooldownHud.bounds(client);
        if(!CombatCooldownHud.visible()||!bounds.contains(x,y))return;
        axial$draggingCooldowns=true;axial$cooldownsOffsetX=(int)x-bounds.x();axial$cooldownsOffsetY=(int)y-bounds.y();cir.setReturnValue(true);
    }
    @Inject(method="method_25403",at=@At("HEAD"),cancellable=true)
    private void axial$dragCooldowns(Click click,double dx,double dy,CallbackInfoReturnable<Boolean> cir){
        if(!axial$draggingCooldowns)return;
        var bounds=CombatCooldownHud.bounds(MinecraftClient.getInstance());
        if(bounds!=null){
            Screen screen=(Screen)(Object)this;
            CooldownsConfig.setPosition(snapPositionX((int)click.x()-axial$cooldownsOffsetX,bounds.width(),Math.max(0,screen.width-bounds.width())),snapPositionY((int)click.y()-axial$cooldownsOffsetY,bounds.height(),Math.max(0,screen.height-bounds.height())));
        }
        cir.setReturnValue(true);
    }
    @Inject(method="method_25406",at=@At("HEAD"),cancellable=true)
    private void axial$releaseCooldowns(Click click,CallbackInfoReturnable<Boolean> cir){
        if(axial$draggingCooldowns&&click.button()==0){axial$saveCooldowns();cir.setReturnValue(true);}
    }
    @Inject(method="method_25419",at=@At("HEAD"))
    private void axial$saveCooldownsOnClose(CallbackInfo ci){axial$saveCooldowns();}
    @Unique private void axial$saveCooldowns(){if(axial$draggingCooldowns){axial$draggingCooldowns=false;CooldownsConfig.save();}}
}
