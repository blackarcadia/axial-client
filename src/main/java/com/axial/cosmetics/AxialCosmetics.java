package com.axial.cosmetics;

import com.axial.cosmetics.client.AlienHuskEntityRenderer;
import com.axial.cosmetics.client.CosmeticFeatureRenderer;
import com.axial.cosmetics.client.CrosshairConfigManager;
import com.axial.cosmetics.client.CrosshairDynamicState;
import com.axial.cosmetics.client.ChunkBordersConfig;
import com.axial.cosmetics.client.ItemScalerConfig;
import com.axial.cosmetics.client.EnchantGlintConfig;
import com.axial.cosmetics.client.MenuMusicConfig;
import com.axial.cosmetics.client.MenuMusicController;
import com.axial.cosmetics.client.WeatherDetectorModelRegistry;
import com.axial.cosmetics.client.CreateWaypointScreen;
import com.axial.cosmetics.client.WaypointWorldRenderer;
import com.axial.cosmetics.data.CosmeticManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.cosmetics.ArmorCosmeticRegistry;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AxialCosmetics implements ClientModInitializer {
    public static final String MOD_ID = "axial_cosmetics";
    private static final CosmeticManager COSMETIC_MANAGER = new CosmeticManager();
    private KeyBinding reloadKey;
    private KeyBinding menuKey;
    private KeyBinding modMenuKey;
    private KeyBinding createWaypointKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityType.HUSK, AlienHuskEntityRenderer::new);

        ArmorCosmeticRegistry.register(247, EquipmentSlot.FEET,
                id("geckolib/models/kroks.geo.json"), id("textures/item/kroks.png"));

        ArmorCosmeticRegistry.register(248, EquipmentSlot.HEAD,
                id("geckolib/models/lampshade.geo.json"), id("textures/item/lampshade.png"));

        ArmorCosmeticRegistry.registerFullModel(
                277,
                EquipmentSlot.LEGS,
                id("geckolib/models/shocktherapy.geo.json"),
                id("textures/item/shocktherapy.png")
        );

        WeatherDetectorModelRegistry.register();
        EnchantGlintConfig.load();
        ItemScalerConfig.load();
        com.axial.cosmetics.client.PotionsHudConfig.load();
        CrosshairConfigManager.load();
        ChunkBordersConfig.load();
        MenuMusicConfig.load();
        WaypointWorldRenderer.register();

        reloadKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, Category.MISC));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, Category.MISC));
        modMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.modmenu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, Category.MISC));
        createWaypointKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.create_waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, Category.MISC));

        try {
            LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
                if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                    registrationHelper.register(new CosmeticFeatureRenderer(playerRenderer, COSMETIC_MANAGER));
                }
            });
        } catch (Throwable t) {
            System.err.println("Axial cosmetic feature renderer disabled: " + t.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MenuMusicController.tick(client);
            ChunkBordersConfig.sync(client);
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
                    openCosmeticMenu(client);
                }
            }
            while (modMenuKey.wasPressed()) {
                if (client.player != null) {
                    openAxialModMenu(client);
                }
            }
            while (createWaypointKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new CreateWaypointScreen());
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

    private void openCosmeticMenu(net.minecraft.client.MinecraftClient client) {
        try {
            Class<?> screenClass = Class.forName("com.axial.cosmetics.client.CosmeticMenuScreen");
            Constructor<?> ctor = screenClass.getConstructor(CosmeticManager.class, net.minecraft.client.MinecraftClient.class);
            Object screen = ctor.newInstance(COSMETIC_MANAGER, client);

            Method setScreen = null;
            for (Method candidate : net.minecraft.client.MinecraftClient.class.getMethods()) {
                if (candidate.getName().equals("setScreen") && candidate.getParameterCount() == 1) {
                    setScreen = candidate;
                    break;
                }
            }
            if (setScreen == null) {
                throw new NoSuchMethodException("MinecraftClient.setScreen method not found");
            }
            setScreen.invoke(client, screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("Unable to open Axial cosmetic menu: " + e.getMessage());
        }
    }

    private void openAxialModMenu(net.minecraft.client.MinecraftClient client) {
        try {
            Class<?> screenClass = Class.forName("org.axial.axialutils.client.AxialConfigScreen");
            java.lang.reflect.Field currentScreenField = net.minecraft.client.MinecraftClient.class.getDeclaredField("currentScreen");
            currentScreenField.setAccessible(true);
            Object parentScreen = currentScreenField.get(client);

            Constructor<?> ctor = null;
            for (Constructor<?> candidate : screenClass.getConstructors()) {
                if (candidate.getParameterCount() == 1) {
                    ctor = candidate;
                    break;
                }
            }
            if (ctor == null) {
                throw new NoSuchMethodException("AxialConfigScreen(Screen) constructor not found");
            }

            Object screen = ctor.newInstance(parentScreen);
            Method setScreen = null;
            for (Method candidate : net.minecraft.client.MinecraftClient.class.getMethods()) {
                if (candidate.getName().equals("setScreen") && candidate.getParameterCount() == 1) {
                    setScreen = candidate;
                    break;
                }
            }
            if (setScreen == null) {
                throw new NoSuchMethodException("MinecraftClient.setScreen method not found");
            }
            setScreen.invoke(client, screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            System.err.println("Unable to open Axial mod menu: " + e.getMessage());
        }
    }
}
