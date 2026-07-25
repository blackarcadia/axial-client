package com.axial.cosmetics.mixin;

import net.minecraft.client.util.Icons;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Mixin(Icons.class)
public abstract class MinecraftIconsMixin {
    private static final String AXIAL_ICON_ROOT = "/assets/axial_cosmetics/icons/";

    @Inject(method = "getIcon", at = @At("HEAD"), cancellable = true)
    private void axial_cosmetics$useAxialIcon(ResourcePack resourcePack, String fileName, CallbackInfoReturnable<InputSupplier<InputStream>> cir) {
        String resourcePath = AXIAL_ICON_ROOT + fileName;
        cir.setReturnValue(() -> {
            InputStream stream = MinecraftIconsMixin.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new FileNotFoundException(resourcePath);
            }
            return stream;
        });
    }
}
