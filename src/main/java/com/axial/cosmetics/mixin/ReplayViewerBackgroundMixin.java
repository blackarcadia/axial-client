package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayMenuTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.lib.de.johni0702.minecraft.gui.container.AbstractGuiScreen$MinecraftGuiScreen",remap=false)
public abstract class ReplayViewerBackgroundMixin {
    // The supplied jar is intermediary; Fabric's development remap uses the named alias.
    @Inject(method={"method_25420","renderBackground"},at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$menuBackground(DrawContext context,int mx,int my,float delta,CallbackInfo ci){if(ReplayMenuTheme.background((Screen)(Object)this,context))ci.cancel();}
}
