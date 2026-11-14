package com.axial.cosmetics.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

/** Emissive and unlit on both faces; alpha blending still preserves the lifetime fade. */
public final class AuraRenderLayers {
    static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(Identifier.of("axial_cosmetics","pipeline/aura_unlit"))
            .withShaderDefine("NO_CARDINAL_LIGHTING").withShaderDefine("NO_OVERLAY")
            .withBlend(BlendFunction.TRANSLUCENT).withCull(false).withDepthWrite(false).build());
    private static RenderLayer layer;
    private AuraRenderLayers() {}
    public static void initialize() {} // Register before shader reload, alongside the other client pipelines.
    static RenderLayer layer(){
        if(layer==null)layer=RenderLayer.of("axial_aura_unlit",RenderSetup.builder(PIPELINE)
                .texture("Sampler0",Identifier.of("axial_cosmetics","textures/entity/auras/white.png"))
                .translucent().expectedBufferSize(1536).build());
        return layer;
    }
}
