package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.OrderedTextTooltipComponentAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.OrderedTextTooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Compact hover hints for Axial menus, rendered in the normal deferred tooltip pass. */
public final class MenuTooltipRenderer {
    private static final Identifier ROUNDED = Identifier.of("axial_cosmetics", "accounts/rounded");
    private static final Identifier OUTLINE = Identifier.of("axial_cosmetics", "accounts/outline");
    private static final int PAD_X = 10, PAD_Y = 7, LINE_HEIGHT = 12;

    private MenuTooltipRenderer() {}

    public static boolean render(DrawContext context, TextRenderer font, List<TooltipComponent> components,
                                 int mouseX, int mouseY, TooltipPositioner positioner, Identifier texture) {
        var screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null || texture != null || components.isEmpty()) return false;
        String screenClass = screen.getClass().getName();
        if (!screenClass.startsWith("com.axial.cosmetics.client.")
                && !screenClass.startsWith("org.axial.axialutils.client.")) return false;
        // Rich item tooltips and custom tooltip textures retain their native layout.
        if (components.stream().anyMatch(component -> !(component instanceof OrderedTextTooltipComponent))) return false;

        List<OrderedText> lines = new ArrayList<>();
        int maxTextWidth = Math.max(1, context.getScaledWindowWidth() - 2 * PAD_X - 8);
        for (TooltipComponent component : components) {
            StringBuilder value = new StringBuilder();
            ((OrderedTextTooltipComponentAccessor) component).axial$getText().accept((index, style, codePoint) -> {
                value.appendCodePoint(codePoint);
                return true;
            });
            lines.addAll(font.wrapLines(UiTypography.text(value.toString(), 1, false), maxTextWidth));
        }
        if (lines.isEmpty()) return false;
        int width = lines.stream().mapToInt(font::getWidth).max().orElse(0) + PAD_X * 2;
        int height = font.fontHeight + (lines.size() - 1) * LINE_HEIGHT + PAD_Y * 2;
        var position = positioner.getPosition(context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                mouseX, mouseY, width, height);
        int x = Math.max(2, Math.min(position.x(), context.getScaledWindowWidth() - width - 2));
        int y = Math.max(2, Math.min(position.y(), context.getScaledWindowHeight() - height - 4));
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ROUNDED, x, y + 2, width, height, 0x50000000);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ROUNDED, x, y, width, height, 0xFA19171F);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, OUTLINE, x, y, width, height, 0xFF88709F);
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(font, lines.get(i), x + PAD_X, y + PAD_Y + i * LINE_HEIGHT, 0xFFF2EFF5, false);
        }
        return true;
    }
}
