package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayMenuTheme;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.lib.de.johni0702.minecraft.gui.element.AbstractGuiButton",remap=false)
public abstract class ReplayViewerButtonsMixin {
    @Inject(method="draw",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$menuButton(@Coerce Object renderer,@Coerce Object size,@Coerce Object info,CallbackInfo ci){if(com.axial.cosmetics.client.ReplayRecordingConfig.hideControl(this)||ReplayMenuTheme.button(this,renderer,size,info))ci.cancel();}
}
