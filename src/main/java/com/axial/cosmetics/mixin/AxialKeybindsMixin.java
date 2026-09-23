package com.axial.cosmetics.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Changes AxialUtils' stock settings key without replacing player keybinds. */
@Mixin(targets = "org.axial.axialutils.client.AxialKeybinds", remap = false)
public abstract class AxialKeybindsMixin {
    @Redirect(
            method = "<clinit>",
            at = @At(value = "NEW", target = "net/minecraft/class_304", remap = false),
            remap = false
    )
    private static KeyBinding axial_cosmetics$useRightShiftForSettings(
            String translationKey, InputUtil.Type type, int keyCode, KeyBinding.Category category) {
        return new KeyBinding(translationKey, type, GLFW.GLFW_KEY_RIGHT_SHIFT, category);
    }
}
