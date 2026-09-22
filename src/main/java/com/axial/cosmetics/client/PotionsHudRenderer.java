package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PotionsHudRenderer {
    private static final int HEADER = 20;
    private static final int ROW_HEIGHT = 24;
    private PotionsHudRenderer() { }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!PotionsHudConfig.isEnabled() || client.options.hudHidden || client.player == null || client.currentScreen != null) return;
        render(context, client, false);
    }

    public static void renderPreview(DrawContext context, MinecraftClient client) {
        if (PotionsHudConfig.isEnabled()) render(context, client, true);
    }

    private static List<StatusEffectInstance> effects(MinecraftClient client, boolean preview) {
        var effects = client.player == null ? new ArrayList<StatusEffectInstance>() : new ArrayList<>(client.player.getStatusEffects());
        effects.sort(Comparator.comparing(effect -> effect.getTranslationKey()));
        if (preview && effects.isEmpty()) effects.add(new StatusEffectInstance(StatusEffects.SPEED, 2400));
        return effects;
    }

    private static Text name(StatusEffectInstance effect) {
        var name = effect.getEffectType().value().getName().copy();
        if (effect.getAmplifier() > 0) name.append(" ").append(Text.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        return name;
    }

    private static Text duration(MinecraftClient client, StatusEffectInstance effect) {
        float tickRate = client.world == null ? 20.0f : client.world.getTickManager().getTickRate();
        return StatusEffectUtil.getDurationText(effect, 1.0f, tickRate);
    }

    public static Bounds bounds(MinecraftClient client, boolean preview) {
        return bounds(client, effects(client, preview));
    }

    private static Bounds bounds(MinecraftClient client, List<StatusEffectInstance> effects) {
        int width = Math.max(120, client.textRenderer.getWidth(PotionsHudConfig.title()) + 12);
        for (var effect : effects) {
            width = Math.max(width, 42 + client.textRenderer.getWidth(name(effect)) + client.textRenderer.getWidth(duration(client, effect)));
        }
        width = Math.min(width, client.getWindow().getScaledWidth());
        int height = HEADER + ROW_HEIGHT * effects.size() + 4;
        return new Bounds(PotionsHudConfig.getX(client.getWindow().getScaledWidth(), width),
                PotionsHudConfig.getY(client.getWindow().getScaledHeight(), height), width, height);
    }

    private static void render(DrawContext context, MinecraftClient client, boolean preview) {
        var effects = effects(client, preview);
        var bounds = bounds(client, effects);
        int x = bounds.x();
        int y = bounds.y();
        if (PotionsHudConfig.showBox()) {
            context.fill(x, y, x + bounds.width(), y + bounds.height(), 0x7A101217);
            context.fill(x + 1, y + 1, x + bounds.width() - 1, y + 2, 0x30FFFFFF);
        }
        context.drawTextWithShadow(client.textRenderer, client.textRenderer.trimToWidth(PotionsHudConfig.title(), Math.max(0, bounds.width() - 12)), x + 6, y + 6, PotionsHudConfig.titleColor());
        int rowY = y + HEADER;
        for (var effect : effects) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(effect.getEffectType()), x + 6, rowY + 2, 18, 18);
            Text duration = duration(client, effect);
            int timeX = x + bounds.width() - 6 - client.textRenderer.getWidth(duration);
            context.drawTextWithShadow(client.textRenderer, client.textRenderer.trimToWidth(name(effect).getString(), Math.max(0, timeX - x - 36)), x + 30, rowY + 7, 0xFFFFFFFF);
            context.drawTextWithShadow(client.textRenderer, duration, timeX, rowY + 7, 0xFFC6D0F3);
            rowY += ROW_HEIGHT;
        }
        if (preview) context.drawStrokedRectangle(x, y, bounds.width(), bounds.height(), 0xD08F5DFF);
    }

    public record Bounds(int x, int y, int width, int height) { }
}
