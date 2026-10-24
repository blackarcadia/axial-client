package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.IrisShaderCompatibility;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum",remap=false)
public abstract class IrisShadowRuntimeCompatibilityMixin {
    @Shadow @Final @Mutable private static boolean FMA_SUPPORT;

    // The pinned Iris initializer only queries UseFMA. Avoid its hard reference to
    // HotSpotDiagnosticMXBean so existing launchers can use Iris's non-FMA path.
    @Inject(method="<clinit>",at=@At("HEAD"),cancellable=true,remap=false)
    private static void axial$optionalHotSpotDiagnostics(CallbackInfo ci){
        FMA_SUPPORT=IrisShaderCompatibility.fmaEnabled();
        ci.cancel();
    }
}
