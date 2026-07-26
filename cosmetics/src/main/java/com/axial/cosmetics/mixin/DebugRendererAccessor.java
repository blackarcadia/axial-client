package com.axial.cosmetics.mixin;

import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DebugRenderer.class)
public interface DebugRendererAccessor {
    @Accessor("renderChunkborder")
    boolean axial_cosmetics$isRenderChunkborder();

    @Invoker("switchRenderChunkborder")
    boolean axial_cosmetics$switchRenderChunkborder();
}
