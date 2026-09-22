package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import org.axial.axialutils.client.AxialUiTheme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PotionsHudRenderer {
    private static final int PADDING = 5;
    private static final int HEADER = 17;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_OFFSET = PADDING + ICON_SIZE + 5;
    // Match Information HUD's translucent background and border.
    private static final int BACKGROUND_COLOR = 2047874077;
    private static final int BORDER_COLOR = -855645094;
    private static final int ROW_HEIGHT = 18;
    private PotionsHudRenderer() { }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!PotionsHudConfig.isEnabled() || client.options.hudHidden || client.player == null || PotionsHudSettingsScreen.isConfigScreen(client.currentScreen)) return;
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
        return hudText(name);
    }

    private static Text duration(MinecraftClient client, StatusEffectInstance effect) {
        float tickRate = client.world == null ? 20.0f : client.world.getTickManager().getTickRate();
        return hudText(StatusEffectUtil.getDurationText(effect, 1.0f, tickRate));
    }

    public static Bounds bounds(MinecraftClient client, boolean preview) {
        return bounds(client, effects(client, preview));
    }

    private static Bounds bounds(MinecraftClient client, List<StatusEffectInstance> effects) {
        int width = client.textRenderer.getWidth(title()) + PADDING * 2;
        for (var effect : effects) {
            width = Math.max(width, TEXT_OFFSET + 4 + PADDING + client.textRenderer.getWidth(name(effect)) + client.textRenderer.getWidth(duration(client, effect)));
        }
        width = Math.min(width, client.getWindow().getScaledWidth());
        int height = HEADER + ROW_HEIGHT * effects.size() + PADDING;
        return new Bounds(PotionsHudConfig.getX(client.getWindow().getScaledWidth(), width),
                PotionsHudConfig.getY(client.getWindow().getScaledHeight(), height), width, height);
    }

    private static void render(DrawContext context, MinecraftClient client, boolean preview) {
        var effects = effects(client, preview);
        if (effects.isEmpty()) return;
        var bounds = bounds(client, effects);
        int x = bounds.x();
        int y = bounds.y();
        if (PotionsHudConfig.showBox()) {
            context.fill(x, y, x + bounds.width(), y + bounds.height(), BACKGROUND_COLOR);
            context.drawStrokedRectangle(x, y, bounds.width(), bounds.height(), BORDER_COLOR);
        }
        context.drawTextWithShadow(client.textRenderer, Language.getInstance().reorder(client.textRenderer.trimToWidth(title(), Math.max(0, bounds.width() - PADDING * 2))), x + PADDING, y + PADDING, PotionsHudConfig.titleColor());
        int rowY = y + HEADER;
        for (var effect : effects) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(effect.getEffectType()), x + PADDING, rowY - 1, ICON_SIZE, ICON_SIZE);
            Text duration = duration(client, effect);
            int timeX = Math.min(x + TEXT_OFFSET + client.textRenderer.getWidth(name(effect)) + 4,
                    x + bounds.width() - PADDING - client.textRenderer.getWidth(duration));
            context.drawTextWithShadow(client.textRenderer, Language.getInstance().reorder(client.textRenderer.trimToWidth(name(effect), Math.max(0, timeX - x - TEXT_OFFSET - 4))), x + TEXT_OFFSET, rowY + 3, 0xFFFFFFFF);
            context.drawTextWithShadow(client.textRenderer, duration, timeX, rowY + 3, 0xFFFFFFFF);
            rowY += ROW_HEIGHT;
        }
    }

    private static Text title() {
        return AxialUiTheme.uiBoldText(PotionsHudConfig.title());
    }

    private static Text hudText(Text text) {
        return AxialUiTheme.uiText(text.getString());
    }

    public record Bounds(int x, int y, int width, int height) { }
}
