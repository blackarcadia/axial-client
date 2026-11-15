package com.axial.cosmetics.mixin;

import java.util.Collection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Axial supplies its own viewer action, so Replay Mod must not rearrange the title buttons. */
@Pseudo
@Mixin(targets="com.replaymod.replay.handler.GuiHandler",remap=false)
public abstract class ReplayMainMenuButtonMixin {
    @Inject(method="injectIntoMainMenu",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$useSubtleReplayButton(Screen screen,Collection<?> buttons,CallbackInfo ci) {
        if(screen instanceof TitleScreen)ci.cancel();
    }
}
