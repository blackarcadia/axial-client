package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class WaypointKeys {
    public static final KeyBinding CREATE = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.axial_cosmetics.waypoint.create", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, Category.MISC));
    public static final KeyBinding EDITOR = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.axial_cosmetics.waypoint.editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, Category.MISC));

    private WaypointKeys() { }
}
