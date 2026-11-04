package com.axial.cosmetics.client;

import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.axial.axialutils.client.AxialConfigManager;

final class ClientProfiles {
    static ProfileStore store(){return new ProfileStore(FabricLoader.getInstance().getConfigDir());}
    static void saveCurrent(){
        AxialConfigManager.save();CrosshairConfigManager.save();PerformanceConfig.save();KeystrokesConfig.save();
        IslandXpHudConfig.save();CooldownsConfig.save();EnchantGlintConfig.save();ThirdPersonNameTagsConfig.save();
        ItemScalerConfig.save();PotionsHudConfig.save();ScoreboardHudConfig.save();InformationHudExtrasConfig.save();
        ChunkBordersConfig.save();MenuMusicConfig.save();MinimapConfig.save();
    }
    static void load(String id) throws IOException {saveCurrent();store().activate(id,ClientProfiles::reload);}
    private static void reload(){
        AxialConfigManager.load();CrosshairConfigManager.load();PerformanceConfig.load();KeystrokesConfig.load();
        IslandXpHudConfig.load();CooldownsConfig.load();EnchantGlintConfig.load();ThirdPersonNameTagsConfig.load();
        ItemScalerConfig.load();PotionsHudConfig.load();ScoreboardHudConfig.load();InformationHudExtrasConfig.load();
        ChunkBordersConfig.load();MenuMusicConfig.load();MinimapConfig.load();
        var client=MinecraftClient.getInstance();
        if(client.world!=null)client.worldRenderer.reload();
        if(!PerformanceConfig.particles())client.particleManager.clearParticles();
    }
}
