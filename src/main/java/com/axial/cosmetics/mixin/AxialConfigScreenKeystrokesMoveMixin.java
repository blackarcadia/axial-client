package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.KeystrokesHud;
import com.axial.cosmetics.client.KeystrokesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="org.axial.axialutils.client.AxialConfigScreen",remap=false,priority=1100)
public abstract class AxialConfigScreenKeystrokesMoveMixin {
    @Unique private boolean axial$draggingKeystrokes;
    @Unique private int axial$keystrokesOffsetX,axial$keystrokesOffsetY;
    @Shadow private int snapPositionX(int desired,int size,int max){throw new AssertionError();}
    @Shadow private int snapPositionY(int desired,int size,int max){throw new AssertionError();}
    @Inject(method="renderMoveMode",at=@At("RETURN"))
    private void axial$previewKeystrokes(DrawContext context,MinecraftClient client,CallbackInfo ci){KeystrokesHud.preview(context,client);}
    @Inject(method="beginMoveDrag",at=@At("HEAD"),cancellable=true)
    private void axial$beginKeystrokesDrag(MinecraftClient client,double x,double y,CallbackInfoReturnable<Boolean> cir){
        var bounds=KeystrokesHud.bounds(client);
        if(!KeystrokesConfig.enabled()||!bounds.contains(x,y))return;
        axial$draggingKeystrokes=true;axial$keystrokesOffsetX=(int)x-bounds.x();axial$keystrokesOffsetY=(int)y-bounds.y();cir.setReturnValue(true);
    }
    @Inject(method="method_25403",at=@At("HEAD"),cancellable=true)
    private void axial$dragKeystrokes(Click click,double dx,double dy,CallbackInfoReturnable<Boolean> cir){
        if(!axial$draggingKeystrokes)return;
        var bounds=KeystrokesHud.bounds(MinecraftClient.getInstance());
        if(bounds!=null){
            Screen screen=(Screen)(Object)this;
            KeystrokesConfig.setPosition(snapPositionX((int)click.x()-axial$keystrokesOffsetX,bounds.width(),Math.max(0,screen.width-bounds.width())),snapPositionY((int)click.y()-axial$keystrokesOffsetY,bounds.height(),Math.max(0,screen.height-bounds.height())));
        }
        cir.setReturnValue(true);
    }
    @Inject(method="method_25406",at=@At("HEAD"),cancellable=true)
    private void axial$releaseKeystrokes(Click click,CallbackInfoReturnable<Boolean> cir){
        if(axial$draggingKeystrokes&&click.button()==0){axial$saveKeystrokes();cir.setReturnValue(true);}
    }
    @Inject(method="method_25419",at=@At("HEAD"))
    private void axial$saveKeystrokesOnClose(CallbackInfo ci){axial$saveKeystrokes();}
    @Unique private void axial$saveKeystrokes(){if(axial$draggingKeystrokes){axial$draggingKeystrokes=false;KeystrokesConfig.save();}}
}
