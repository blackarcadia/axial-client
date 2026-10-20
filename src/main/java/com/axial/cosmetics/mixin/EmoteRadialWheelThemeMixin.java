package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses a pre-rendered wheel texture so the custom UI stays smooth while Emotecraft draws real icons. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget", remap = false)
public abstract class EmoteRadialWheelThemeMixin {
    private static final int SEGMENT_COUNT = 8;
    private static final int WHEEL_TEXTURE_SIZE = 1254;
    private static final Identifier WHEEL_TEXTURE = Identifier.of("axial_cosmetics", "textures/gui/emote_wheel.png");
    private static final Identifier HOVER_TEXTURE = Identifier.of("axial_cosmetics", "textures/gui/emote_wheel_hover.png");
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    private static int previousHoveredSegment = -1;
    private static long hoverChangedAtNanos;
    private static float activeHoverProgress;

    @Inject(method = "method_48579", at = @At("HEAD"), remap = false)
    private void axial_cosmetics$drawAxialWheel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) return;

        ClickableWidget widget = (ClickableWidget) (Object) this;
        int size = Math.min(widget.getWidth(), widget.getHeight());
        int centerX = widget.getX() + widget.getWidth() / 2;
        int centerY = widget.getY() + widget.getHeight() / 2;
        int radius = Math.round(size * 0.47f);
        int diameter = radius * 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WHEEL_TEXTURE,
                centerX - radius, centerY - radius, 0.0f, 0.0f, diameter, diameter,
                WHEEL_TEXTURE_SIZE, WHEEL_TEXTURE_SIZE, WHEEL_TEXTURE_SIZE, WHEEL_TEXTURE_SIZE);

        drawCenterGlyph(context, centerX, centerY, radius);
        drawSlotLabels(context, MinecraftClient.getInstance(), centerX, centerY, radius);
        int hoveredSegment = segmentAt(centerX, centerY, Math.round(radius * 0.34f), radius, mouseX, mouseY);
        float hoverProgress = hoverProgress(hoveredSegment);
        activeHoverProgress = hoverProgress;
        if (hoveredSegment >= 0) drawHoverCard(context, centerX, centerY, radius, hoveredSegment, hoverProgress);
        if (hoveredSegment >= 0) {
            drawTooltip(context, MinecraftClient.getInstance(), (AbstractFastChooseWidget) (Object) this,
                    hoveredSegment, centerX, centerY, radius, hoverProgress);
        }
    }

    private static float hoverProgress(int hoveredSegment) {
        if (hoveredSegment != previousHoveredSegment) {
            previousHoveredSegment = hoveredSegment;
            hoverChangedAtNanos = System.nanoTime();
        }
        if (hoveredSegment < 0) return 0.0f;
        float progress = Math.min(1.0f, (System.nanoTime() - hoverChangedAtNanos) / 130_000_000.0f);
        return progress * progress * (3.0f - 2.0f * progress);
    }

    public static float axial_cosmetics$iconHoverScale(AbstractFastChooseWidget wheel, IChooseElement element) {
        int elementIndex = wheel.getChooseElements().indexOf(element);
        return elementIndex == previousHoveredSegment ? 1.0f + activeHoverProgress * 0.08f : 1.0f;
    }

    private static void drawHoverCard(DrawContext context, int centerX, int centerY, int radius,
                                      int hoveredSegment, float progress) {
        float rotation = (1 - hoveredSegment) * (float) (Math.PI / 4.0);
        float scale = 1.0f + 0.035f * progress;
        context.getMatrices().pushMatrix();
        context.getMatrices().rotateAbout(rotation, centerX, centerY);
        context.getMatrices().scaleAround(scale, centerX, centerY);
        int diameter = radius * 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, HOVER_TEXTURE,
                centerX - radius, centerY - radius, 0.0f, 0.0f, diameter, diameter,
                WHEEL_TEXTURE_SIZE, WHEEL_TEXTURE_SIZE, WHEEL_TEXTURE_SIZE, WHEEL_TEXTURE_SIZE);
        context.getMatrices().popMatrix();
    }

    private static void drawCenterGlyph(DrawContext context, int centerX, int centerY, int radius) {
        int unit = Math.max(3, radius / 22);
        context.fill(centerX - unit * 3, centerY - unit, centerX - unit, centerY + unit, 0xFFF6F4FF);
        context.fill(centerX + unit, centerY - unit, centerX + unit * 3, centerY + unit, 0xFFF6F4FF);
        context.fill(centerX - unit * 3, centerY + unit * 2, centerX + unit * 3, centerY + unit * 3, 0xFFF6F4FF);
    }

    private static int segmentAt(int centerX, int centerY, int innerRadius, int outerRadius, int mouseX, int mouseY) {
        double x = mouseX - centerX;
        double y = mouseY - centerY;
        double distance = Math.hypot(x, y);
        if (distance < innerRadius || distance > outerRadius) return -1;
        double segmentSize = Math.PI * 2 / SEGMENT_COUNT;
        return Math.floorMod((int) Math.floor((Math.PI / 2 - Math.atan2(y, x) + segmentSize / 2) / segmentSize), SEGMENT_COUNT);
    }

    private static void drawSlotLabels(DrawContext context, MinecraftClient client, int centerX, int centerY, int radius) {
        int labelRadius = radius + 16;
        for (int index = 0; index < SEGMENT_COUNT; index++) {
            double angle = Math.PI / 2 - Math.PI * 2 * index / SEGMENT_COUNT;
            int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
            context.fill(x - 9, y - 9, x + 10, y + 10, 0xEF171725);
            context.drawStrokedRectangle(x - 9, y - 9, 19, 19, 0xFF8E5EFF);
            context.drawCenteredTextWithShadow(client.textRenderer, uiText(Integer.toString(index + 1)), x, y - 4, 0xFFF2EEFF);
        }
    }

    private static void drawTooltip(DrawContext context, MinecraftClient client, AbstractFastChooseWidget wheel,
                                    int hoveredSegment, int centerX, int centerY, int radius, float progress) {
        if (hoveredSegment >= wheel.getChooseElements().size()) return;
        IChooseElement element = wheel.getChooseElements().get(hoveredSegment);
        if (!element.hasEmote() || element.getEmote() == null) return;
        String name = element.getEmote().name.getString();
        int width = Math.max(112, client.textRenderer.getWidth(name) + 28);
        int x = centerX - width / 2;
        int y = centerY + radius + 36 + Math.round((1.0f - progress) * 8.0f);
        context.fill(x - 2, y - 2, x + width + 2, y + 22, 0xFF38DDF5);
        context.fill(x, y, x + width, y + 20, 0xF0101420);
        context.drawCenteredTextWithShadow(client.textRenderer, uiText(name), centerX, y + 6, 0xFFF6F4FF);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
