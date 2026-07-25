package com.axial.cosmetics;

import com.axial.cosmetics.client.CosmeticFeatureRenderer;
import com.axial.cosmetics.client.CrosshairConfigManager;
import com.axial.cosmetics.client.CrosshairDynamicState;
import com.axial.cosmetics.client.ItemScalerConfig;
import com.axial.cosmetics.client.MenuMusicConfig;
import com.axial.cosmetics.client.MenuMusicController;
import com.axial.cosmetics.data.CosmeticManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;

public class AxialCosmetics implements ClientModInitializer {
    public static final String MOD_ID = "axial_cosmetics";
    private static final CosmeticManager COSMETIC_MANAGER = new CosmeticManager();
    private KeyBinding reloadKey;
    private KeyBinding menuKey;
    private KeyBinding modMenuKey;

    @Override
    public void onInitializeClient() {
        ItemScalerConfig.load();
        CrosshairConfigManager.load();
        MenuMusicConfig.load();

        reloadKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, Category.MISC));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, Category.MISC));
        modMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.modmenu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, Category.MISC));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new CosmeticFeatureRenderer(playerRenderer, COSMETIC_MANAGER));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MenuMusicController.tick(client);
            if (client.player != null && client.currentScreen == null && CrosshairConfigManager.get().enabled && CrosshairConfigManager.get().dynamicEnabled) {
                if (client.options.attackKey.wasPressed() || client.options.useKey.wasPressed()) {
                    CrosshairDynamicState.triggerPulse();
                }
            }
            while (reloadKey.wasPressed()) {
                COSMETIC_MANAGER.reload(client);
                System.out.println("[AxialCosmetics] Reloaded cosmetics.");
            }
            while (menuKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new com.axial.cosmetics.client.CosmeticMenuScreen(COSMETIC_MANAGER, client));
                }
            }
            while (modMenuKey.wasPressed()) {
                if (client.player != null) {
                    openAxialModMenu(client);
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null && !COSMETIC_MANAGER.isInitialized()) {
                COSMETIC_MANAGER.reload(client);
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    private void openAxialModMenu(net.minecraft.client.MinecraftClient client) {
        try {
            Class<?> screenClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            Constructor<?> ctor = screenClass.getConstructor(net.minecraft.client.gui.screen.Screen.class);
            Object screen = ctor.newInstance(client.currentScreen);
            if (screen instanceof net.minecraft.client.gui.screen.Screen axialScreen) {
                client.setScreen(axialScreen);
            }
        } catch (ReflectiveOperationException e) {
            System.err.println("Unable to open Axial mod menu: " + e.getMessage());
        }
    }
}
