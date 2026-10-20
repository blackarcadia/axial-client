package com.axial.cosmetics.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Leaves the debug dump-version action unbound on fresh installs. */
@Mixin(net.minecraft.client.option.GameOptions.class)
public abstract class DebugDumpVersionKeyMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "NEW", target = "net/minecraft/client/option/KeyBinding")
    )
    private KeyBinding axial_cosmetics$unbindDumpVersion(
            String translationKey, InputUtil.Type type, int keyCode, KeyBinding.Category category) {
        int defaultKey = "key.debug.dumpVersion".equals(translationKey) ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
        return new KeyBinding(translationKey, type, defaultKey, category);
    }
}
