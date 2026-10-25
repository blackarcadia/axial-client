package com.axial.cosmetics.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Changes AxialUtils' stock settings key without replacing player keybinds. */
@Mixin(targets = "org.axial.axialutils.client.AxialKeybinds", remap = false)
public abstract class AxialKeybindsMixin {
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = GLFW.GLFW_KEY_BACKSLASH), remap = false)
    private static int axial_cosmetics$useRightShiftForSettings(int original) {
        return GLFW.GLFW_KEY_RIGHT_SHIFT;
    }
}
