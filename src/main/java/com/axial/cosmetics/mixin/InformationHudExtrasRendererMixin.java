package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HudColorDefaults;
import com.axial.cosmetics.client.InformationHudExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.axial.axialutils.client.AxialHudRenderer", remap = false)
public abstract class InformationHudExtrasRendererMixin {
    @Inject(method = "renderBox", at = @At("RETURN"), remap = false)
    private static void axial_cosmetics$renderExtraLines(DrawContext context, MinecraftClient client, int x, int y,
                                                           boolean preview, CallbackInfo ci) {
        int extras = InformationHudExtrasConfig.enabledCount();
        if (extras == 0) return;

        int baseLines = 1 + (AxialConfigManager.get().showPickaxeChargePerMinute ? 1 : 0)
                + (AxialConfigManager.get().showMiningXpPerMinute ? 1 : 0);
        int lineY = y + 5 + baseLines * 11 + 4;
        if (InformationHudExtrasConfig.showServer()) lineY = draw(context, client, x, lineY, "Server: " + server(client, preview));
        if (InformationHudExtrasConfig.showFacing()) lineY = draw(context, client, x, lineY, "Facing: " + facing(client, preview));
        if (InformationHudExtrasConfig.showCoordinates()) lineY = draw(context, client, x, lineY, "Coordinates: " + coordinates(client, preview));
        if (InformationHudExtrasConfig.showPing()) draw(context, client, x, lineY, "Ping: " + ping(client, preview) + " ms");
    }

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true, remap = false)
    private static void axial_cosmetics$includeExtraLineHeight(MinecraftClient client, CallbackInfoReturnable<Integer> cir) {
        int extras = InformationHudExtrasConfig.enabledCount();
        if (extras == 0) return;
        int baseLines = 1 + (AxialConfigManager.get().showPickaxeChargePerMinute ? 1 : 0)
                + (AxialConfigManager.get().showMiningXpPerMinute ? 1 : 0);
        cir.setReturnValue(cir.getReturnValue() + extras * 11 + (baseLines == 1 ? 4 : 0));
    }

    private static int draw(DrawContext context, MinecraftClient client, int x, int y, String line) {
        context.drawTextWithShadow(client.textRenderer, Text.literal(line), x + 5, y, HudColorDefaults.SUBTITLE);
        return y + 11;
    }

    private static String server(MinecraftClient client, boolean preview) {
        if (preview) return "play.example.net";
        return client.getCurrentServerEntry() == null ? "Singleplayer" : client.getCurrentServerEntry().address;
    }

    private static String facing(MinecraftClient client, boolean preview) {
        if (preview || client.player == null) return "North";
        return client.player.getHorizontalFacing().asString().toUpperCase(java.util.Locale.ROOT);
    }

    private static String coordinates(MinecraftClient client, boolean preview) {
        if (preview || client.player == null) return "0, 64, 0";
        return client.player.getBlockX() + ", " + client.player.getBlockY() + ", " + client.player.getBlockZ();
    }

    private static int ping(MinecraftClient client, boolean preview) {
        if (preview || client.player == null || client.getNetworkHandler() == null) return 42;
        var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }
}
