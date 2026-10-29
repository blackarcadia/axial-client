package com.axial.cosmetics;

import com.axial.cosmetics.client.AlienHuskEntityRenderer;
import com.axial.cosmetics.client.BundledEmoteSynchronizer;
import com.axial.cosmetics.client.CosmeticFeatureRenderer;
import com.axial.cosmetics.client.CrosshairConfigManager;
import com.axial.cosmetics.client.CrosshairDynamicState;
import com.axial.cosmetics.client.ChunkBordersConfig;
import com.axial.cosmetics.client.ItemScalerConfig;
import com.axial.cosmetics.client.EnchantGlintConfig;
import com.axial.cosmetics.client.InformationHudExtrasConfig;
import com.axial.cosmetics.client.MenuMusicConfig;
import com.axial.cosmetics.client.MenuMusicController;
import com.axial.cosmetics.client.MinimapFullScreen;
import com.axial.cosmetics.client.MinimapExploration;
import com.axial.cosmetics.client.MinimapMapPayload;
import com.axial.cosmetics.client.MinimapRenderer;
import com.axial.cosmetics.client.MinimapServerOverview;
import com.axial.cosmetics.client.WeatherDetectorModelRegistry;
import com.axial.cosmetics.client.ThirdPersonNameTagsConfig;
import com.axial.cosmetics.client.CreateWaypointScreen;
import com.axial.cosmetics.client.VerifiedMobAttack;
import com.axial.cosmetics.client.WaypointWorldRenderer;
import com.axial.cosmetics.data.CosmeticManager;
import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.main.config.ClientConfig;
import io.github.kosmx.emotes.main.config.CloseWheel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
    private static KeyBinding createWaypointKey;
    private static KeyBinding chunkBordersToggleKey;
    private static KeyBinding minimapMapKey;
    private static boolean minimapPlayerWasDead;

    @Override
    public void onInitializeClient() {
        BundledEmoteSynchronizer.synchronize();
        com.axial.cosmetics.client.EmoteMenuTheme.initialize();
        PayloadTypeRegistry.playC2S().register(com.axial.cosmetics.client.GangDirectoryPayload.ID,com.axial.cosmetics.client.GangDirectoryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.axial.cosmetics.client.GangDirectoryPayload.ID,com.axial.cosmetics.client.GangDirectoryPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(com.axial.cosmetics.client.GangDirectoryPayload.ID,(payload,context)->{
            var client=context.client();var connection=client.getNetworkHandler();
            client.execute(()->{if(client.getNetworkHandler()==connection&&client.currentScreen instanceof com.axial.cosmetics.client.GangsScreen)com.axial.cosmetics.client.GangDirectoryGuide.receive(payload.data());});
        });

        com.axial.cosmetics.client.PerformanceConfig.load();
        com.axial.cosmetics.client.KeystrokesConfig.load();
        com.axial.cosmetics.client.IslandXpHudConfig.load();
        com.axial.cosmetics.client.CooldownsConfig.load();
        PayloadTypeRegistry.playS2C().register(com.axial.cosmetics.client.CombatCooldownPayload.ID,com.axial.cosmetics.client.CombatCooldownPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(com.axial.cosmetics.client.CombatCooldownPayload.ID,(payload,context)->{
            var client=context.client();var connection=client.getNetworkHandler();
            client.execute(()->{if(client.getNetworkHandler()==connection)com.axial.cosmetics.client.CombatCooldownHud.receive(payload.data());});
        });

        PayloadTypeRegistry.playC2S().register(com.axial.cosmetics.client.EnchantmentPayload.ID, com.axial.cosmetics.client.EnchantmentPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.axial.cosmetics.client.EnchantmentPayload.ID, com.axial.cosmetics.client.EnchantmentPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(com.axial.cosmetics.client.EnchantmentPayload.ID, (payload, context) -> {
            var client=context.client();var connection=client.getNetworkHandler();
            client.execute(() -> {if(client.getNetworkHandler()==connection)com.axial.cosmetics.client.EnchantmentGuide.receive(payload.data());});
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{
            com.axial.cosmetics.client.EnchantmentGuide.reset();com.axial.cosmetics.client.GangDirectoryGuide.reset();com.axial.cosmetics.client.PetCooldownOverlay.reset();com.axial.cosmetics.client.CombatCooldownHud.reset();
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            com.axial.cosmetics.client.EnchantmentGuide.reset();com.axial.cosmetics.client.GangDirectoryGuide.reset();com.axial.cosmetics.client.PetCooldownOverlay.reset();com.axial.cosmetics.client.CombatCooldownHud.reset();
        });

        PayloadTypeRegistry.playC2S().register(com.axial.cosmetics.client.WorldMapPayload.ID, com.axial.cosmetics.client.WorldMapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.axial.cosmetics.client.WorldMapPayload.ID, com.axial.cosmetics.client.WorldMapPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(com.axial.cosmetics.client.WorldMapPayload.ID, (payload, context) -> {
            var client=context.client();var world=client.world;var connection=client.getNetworkHandler();
            client.execute(() -> {
                if(client.world!=world||client.getNetworkHandler()!=connection||!MinimapRenderer.mapVisible(client))return;
                MinimapRenderer.syncWorld(client);
                MinimapServerOverview.receiveFullWorld(payload.data());
            });
        });
        PayloadTypeRegistry.playC2S().register(MinimapMapPayload.ID, MinimapMapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MinimapMapPayload.ID, MinimapMapPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MinimapMapPayload.ID, (payload, context) -> {
            var client=context.client();var world=client.world;var connection=client.getNetworkHandler();
            client.execute(() -> {
                if(client.world!=world||client.getNetworkHandler()!=connection||!MinimapRenderer.mapVisible(client))return;
                MinimapRenderer.syncWorld(client);
                MinimapServerOverview.receive(payload.data());
            });
        });
        applyEmotecraftDefaults();
        EntityRendererRegistry.register(EntityType.HUSK, AlienHuskEntityRenderer::new);

        ArmorCosmeticRegistry.register(247, EquipmentSlot.FEET,
                id("geckolib/models/kroks.geo.json"), id("textures/item/kroks.png"));

        ArmorCosmeticRegistry.register(281, EquipmentSlot.CHEST,
                id("geckolib/models/broodmother.geo.json"), id("textures/item/broodmother.png"));

        ArmorCosmeticRegistry.register(292, EquipmentSlot.HEAD,
                id("geckolib/models/knightshelmet.geo.json"), id("textures/item/knightshelmet.png"));

        ArmorCosmeticRegistry.register(248, EquipmentSlot.HEAD,
                id("geckolib/models/lampshade.geo.json"), id("textures/item/lampshade.png"));

        ArmorCosmeticRegistry.register(293, EquipmentSlot.CHEST,
                id("geckolib/models/diamondnecklace.geo.json"), id("textures/item/diamondnecklace.png"));


        ArmorCosmeticRegistry.register(295, EquipmentSlot.FEET,
                id("geckolib/models/knightsboots.geo.json"), id("textures/item/knightsboots.png"));

        ArmorCosmeticRegistry.registerFullModel(
                294,
                EquipmentSlot.LEGS,
                id("geckolib/models/chainbelt.geo.json"),
                id("textures/item/chainbelt.png")
        );

        ArmorCosmeticRegistry.registerFullModel(
                277,
                EquipmentSlot.LEGS,
                id("geckolib/models/shocktherapy.geo.json"),
                id("textures/item/shocktherapy.png")
        );

        WeatherDetectorModelRegistry.register();
        EnchantGlintConfig.load();
        ThirdPersonNameTagsConfig.load();
        ItemScalerConfig.load();
        com.axial.cosmetics.client.PotionsHudConfig.load();
        com.axial.cosmetics.client.ScoreboardHudConfig.load();
        InformationHudExtrasConfig.load();
        CrosshairConfigManager.load();
        ChunkBordersConfig.load();
        MenuMusicConfig.load();
        com.axial.cosmetics.client.ClientPresence.start();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> com.axial.cosmetics.client.ClientPresence.stop());
        WaypointWorldRenderer.register();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->MinimapRenderer.refreshWorld(client));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client,world)->MinimapRenderer.refreshWorld(client));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            com.axial.cosmetics.client.IslandXpPopup.clear();
            MinimapExploration.flush();MinimapRenderer.refreshWorld(client);
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client->MinimapExploration.shutdown());

        reloadKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, Category.MISC));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, Category.MISC));
        modMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.modmenu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, Category.MISC));
        createWaypointKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.create_waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, Category.MISC));
        chunkBordersToggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.chunk_borders_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, Category.MISC));
        minimapMapKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.axial_cosmetics.minimap_full_map", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, Category.MISC));

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
            com.axial.cosmetics.client.EnchantmentGuide.tick(client);
            com.axial.cosmetics.client.GangDirectoryGuide.tick(client);
            MinimapRenderer.tick(client);
            applyEmotecraftDefaults();
            MenuMusicController.tick(client);
            ChunkBordersConfig.sync(client);
            if (client.player != null) {
                if (client.player.isDead() && !minimapPlayerWasDead) {
                    MinimapExploration.markDeath(client);
                }
                minimapPlayerWasDead = client.player.isDead();
            } else {
                minimapPlayerWasDead = false;
            }
            MinimapExploration.saveIfDue();
            VerifiedMobAttack.tick(client);
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
            while (chunkBordersToggleKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    ChunkBordersConfig.setEnabled(!ChunkBordersConfig.enabled(), client);
                }
            }
            while (minimapMapKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new MinimapFullScreen());
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null && !COSMETIC_MANAGER.isInitialized()) {
                COSMETIC_MANAGER.reload(client);
            }
        });
    }

    private static void applyEmotecraftDefaults() {
        ClientConfig config = PlatformTools.getConfig();
        if (config == null) return;
        config.closeWheelType.set(CloseWheel.HOLD);
        config.oldChooseWheel.set(true);
        config.showIconsIfPossible.set(true);
        config.loadBuiltinEmotes.set(false);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static KeyBinding chunkBordersToggleKey() {
        return chunkBordersToggleKey;
    }

    public static KeyBinding createWaypointKey() {
        return createWaypointKey;
    }

    public static KeyBinding minimapMapKey() {
        return minimapMapKey;
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
