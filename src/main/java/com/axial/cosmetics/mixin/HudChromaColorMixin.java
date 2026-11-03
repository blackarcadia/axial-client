package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ChromaColor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Resolve colours at their render-time field reads, never by mutating saved configuration. */
@Mixin(targets={"org.axial.axialutils.client.AxialHudRenderer",
        "org.axial.axialutils.client.SatchelHudRenderer",
        "org.axial.axialutils.client.CpsHudRenderer",
        "org.axial.axialutils.client.ArmorHudRenderer"},remap=false)
public abstract class HudChromaColorMixin {
    // Each renderer owns only a subset of these settings; coverage is checked against the bundled HUD classes.
    @ModifyExpressionValue(method="*",at={
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;informationHudTitleColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;informationHudChargeColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;informationHudXpColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;satchelHudTitleColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;satchelHudCountColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;satchelHudEmptyColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;cpsHudTitleColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;cpsHudColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;armorHudColor:I",remap=false),
            @At(value="FIELD",target="Lorg/axial/axialutils/client/AxialConfig;armorHudDurabilityColor:I",remap=false)
    },require=0,remap=false)
    private static int axial$resolveChroma(int configured){return ChromaColor.resolve(configured);}
}
