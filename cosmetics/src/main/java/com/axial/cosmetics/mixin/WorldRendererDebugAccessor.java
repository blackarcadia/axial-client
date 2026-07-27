package com.axial.cosmetics.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererDebugAccessor {
    @Accessor("debugRenderer")
    DebugRenderer axial_cosmetics$getDebugRenderer();
}
