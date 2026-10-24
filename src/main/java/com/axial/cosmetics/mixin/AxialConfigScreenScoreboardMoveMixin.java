package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ScoreboardHud;
import com.axial.cosmetics.client.ScoreboardHudConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="org.axial.axialutils.client.AxialConfigScreen",remap=false,priority=1100)
public abstract class AxialConfigScreenScoreboardMoveMixin {
    @Unique private boolean axial$draggingScoreboard;
    @Unique private int axial$scoreboardOffsetX,axial$scoreboardOffsetY;
    @Shadow private int snapPositionX(int desired,int size,int max){throw new AssertionError();}
    @Shadow private int snapPositionY(int desired,int size,int max){throw new AssertionError();}
    @Inject(method="renderMoveMode",at=@At("RETURN"))
    private void axial$previewScoreboard(DrawContext context,MinecraftClient client,CallbackInfo ci){ScoreboardHud.renderPreview(context,client);}
    @Inject(method="beginMoveDrag",at=@At("HEAD"),cancellable=true)
    private void axial$beginScoreboardDrag(MinecraftClient client,double x,double y,CallbackInfoReturnable<Boolean> cir){
        var bounds=ScoreboardHud.bounds();
        if(!ScoreboardHud.enabled()||bounds==null||!bounds.contains(x,y))return;
        axial$draggingScoreboard=true;axial$scoreboardOffsetX=(int)x-bounds.x();axial$scoreboardOffsetY=(int)y-bounds.y();cir.setReturnValue(true);
    }
    @Inject(method="method_25403",at=@At("HEAD"),cancellable=true)
    private void axial$dragScoreboard(Click click,double dx,double dy,CallbackInfoReturnable<Boolean> cir){
        if(!axial$draggingScoreboard)return;
        var bounds=ScoreboardHud.bounds();
        if(bounds!=null){
            Screen screen=(Screen)(Object)this;
            ScoreboardHudConfig.setPosition(snapPositionX((int)click.x()-axial$scoreboardOffsetX,bounds.width(),Math.max(0,screen.width-bounds.width())),snapPositionY((int)click.y()-axial$scoreboardOffsetY,bounds.height(),Math.max(0,screen.height-bounds.height())));
        }
        cir.setReturnValue(true);
    }
    @Inject(method="method_25406",at=@At("HEAD"),cancellable=true)
    private void axial$releaseScoreboard(Click click,CallbackInfoReturnable<Boolean> cir){
        if(axial$draggingScoreboard&&click.button()==0){axial$saveScoreboard();cir.setReturnValue(true);}
    }
    @Inject(method="method_25419",at=@At("HEAD"))
    private void axial$saveScoreboardOnClose(CallbackInfo ci){axial$saveScoreboard();}
    @Unique private void axial$saveScoreboard(){if(axial$draggingScoreboard){axial$draggingScoreboard=false;ScoreboardHudConfig.save();}}
}
