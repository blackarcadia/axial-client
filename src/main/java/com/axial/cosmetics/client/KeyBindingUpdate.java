package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

final class KeyBindingUpdate {
    private KeyBindingUpdate() { }
    static void save() {
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
    }
}
