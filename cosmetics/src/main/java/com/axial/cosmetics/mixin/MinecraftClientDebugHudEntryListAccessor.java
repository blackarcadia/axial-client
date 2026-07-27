package com.axial.cosmetics.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientDebugHudEntryListAccessor {
    @Accessor("debugHudEntryList")
    DebugHudProfile axial_cosmetics$getDebugHudEntryList();
}
