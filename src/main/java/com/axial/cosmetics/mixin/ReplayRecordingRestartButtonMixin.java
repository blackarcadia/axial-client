package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayRecordingRestart;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class ReplayRecordingRestartButtonMixin extends Screen {
    @Unique private ButtonWidget axial$startRecording;
    protected ReplayRecordingRestartButtonMixin(Text title){super(title);}
    @Shadow public abstract boolean shouldShowMenu();
    @Inject(method="init",at=@At("TAIL"))
    private void axial$restoreStartButton(CallbackInfo ci) {
        axial$startRecording=null;if(!shouldShowMenu())return;
        int y=height/4+128;
        for(var child:children())if(child instanceof ClickableWidget button&&button.getMessage().getContent() instanceof TranslatableTextContent value
                &&(value.getKey().equals("menu.disconnect")||value.getKey().equals("menu.returnToMenu")))y=button.getY()+button.getHeight()+4;
        axial$startRecording=addDrawableChild(ButtonWidget.builder(com.axial.cosmetics.client.ClientMenuText.literal("Start Recording"),b->ReplayRecordingRestart.open(this)).dimensions(width/2-100,y,200,20).build());
        axial$startRecording.visible=axial$startRecording.active=ReplayRecordingRestart.showStart();
    }
    @Inject(method="render",at=@At("HEAD"))
    private void axial$refreshStartButton(DrawContext context,int mx,int my,float delta,CallbackInfo ci) {
        if(axial$startRecording!=null)axial$startRecording.visible=axial$startRecording.active=ReplayRecordingRestart.showStart();
    }
}
