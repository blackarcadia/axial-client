package com.axial.cosmetics.mixin;

import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep ImmediatelyFast's map atlas usable on older trimmed launcher runtimes. */
@Mixin(targets = "net.minecraft.client.texture.MapTextureManager$MapTexture", priority = 900)
public abstract class ImmediatelyFastMapRuntimeCompatibilityMixin {
    // ImmediatelyFast merges a shared placeholder into this class at priority 1000.
    // Reflect.Objects.allocate initializes HotSpotDiagnosticMXBean even though the
    // placeholder needs no JVM diagnostics. Use a normal, tiny texture instead.
    // Optional because vanilla maps have no allocation call when the atlas is off.
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lnet/lenni0451/reflect/Objects;allocate(Ljava/lang/Class;)Ljava/lang/Object;", remap = false), require = 0)
    private static Object axial$createMapPlaceholder(Class<?> ignored) {
        return new NativeImageBackedTexture(() -> "Axial map atlas placeholder", 1, 1, true);
    }
}
