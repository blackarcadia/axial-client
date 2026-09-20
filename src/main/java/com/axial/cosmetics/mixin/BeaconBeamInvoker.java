package com.axial.cosmetics.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BeaconBlockEntityRenderer.class)
public interface BeaconBeamInvoker {
    @Invoker("renderBeam")
    static void axial_cosmetics$renderBeam(MatrixStack matrices, OrderedRenderCommandQueue queue, float tickProgress, float scale, int worldTime, int yOffset, int maxHeight) {
        throw new AssertionError();
    }
}
