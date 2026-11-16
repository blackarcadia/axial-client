package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayRecordingConfig;
import java.util.Collection;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.recording.gui.GuiRecordingControls",remap=false)
public abstract class ReplayRecordingControlsMixin {
    @Inject(method="updateState",at=@At("TAIL"),remap=false)
    private void axial$trackExistingControls(CallbackInfo ci){ReplayRecordingConfig.registerControls(this);}
    @Inject(method={"lambda$new$1","lambda$new$3"},at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$blockStaleControls(CallbackInfo ci){if(!ReplayRecordingConfig.controlsEnabled(this))ci.cancel();}
    @Inject(method="injectIntoIngameMenu",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$hideDisabledRecordingControls(Screen screen,Collection<?> buttons,CallbackInfo ci){if(!ReplayRecordingConfig.enabled())ci.cancel();}
}
