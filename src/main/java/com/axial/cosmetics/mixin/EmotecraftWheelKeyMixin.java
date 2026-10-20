package com.axial.cosmetics.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sets Emotecraft's wheel binding to V for fresh installs. */
@Mixin(targets = "io.github.kosmx.emotes.arch.EmotecraftClientMod", remap = false)
public abstract class EmotecraftWheelKeyMixin {
    @Redirect(
            method = "<clinit>",
            at = @At(value = "NEW", target = "net/minecraft/class_304", remap = false),
            remap = false
    )
    private static KeyBinding axial_cosmetics$useVForEmoteWheel(
            String translationKey, InputUtil.Type type, int keyCode, KeyBinding.Category category) {
        int defaultKey = "key.emotecraft.fastchoose".equals(translationKey) ? GLFW.GLFW_KEY_V : keyCode;
        return new KeyBinding(translationKey, type, defaultKey, category);
    }
}
