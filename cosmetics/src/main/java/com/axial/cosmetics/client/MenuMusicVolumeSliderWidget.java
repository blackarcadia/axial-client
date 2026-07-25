package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class MenuMusicVolumeSliderWidget extends ClickableWidget {
    private static final int TRACK_COLOR = 0xCC11151D;
    private static final int TRACK_BORDER_COLOR = 0xFF2A2D36;
    private static final int TRACK_FILL_COLOR = 0xFFE54FFF;
    private static final int HANDLE_COLOR = 0xFFF6E7FF;
    private static final int HANDLE_BORDER_COLOR = 0xFF4A145F;
    private static final int PADDING = 2;

    private double value;

    public MenuMusicVolumeSliderWidget(int x, int y, int width, int height, float initialValue) {
        super(x, y, width, height, Text.literal("Menu Music Volume"));
        this.value = clamp(initialValue);
        updateMessage();
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        setValueFromMouse(click.x());
    }

    @Override
    public void onDrag(Click click, double offsetX, double offsetY) {
        setValueFromMouse(click.x());
    }

    @Override
    public void onRelease(Click click) {
        setValueFromMouse(click.x());
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        int innerX = x + 1;
        int innerY = y + 1;
        int innerWidth = width - 2;
        int innerHeight = height - 2;
        int fillWidth = (int) Math.round((innerWidth - (PADDING * 2)) * value);
        int fillX = innerX + PADDING;
        int fillY = innerY + PADDING;
        int fillHeight = innerHeight - (PADDING * 2);
        int handleX = Math.min(fillX + fillWidth - 2, x + width - 6);
        boolean hovered = this.isHovered();

        context.fill(x, y, x + width, y + height, 0xA00B0D12);
        context.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, TRACK_COLOR);
        context.fill(innerX, innerY, innerX + innerWidth, innerY + 1, TRACK_BORDER_COLOR);
        context.fill(fillX, fillY, fillX + Math.max(0, fillWidth), fillY + fillHeight, TRACK_FILL_COLOR);
        context.fill(handleX, y + 2, handleX + 4, y + height - 2, hovered ? HANDLE_COLOR : 0xFFEAD3FF);
        context.drawStrokedRectangle(handleX, y + 2, 4, height - 4, HANDLE_BORDER_COLOR);
        context.drawStrokedRectangle(x, y, width, height, TRACK_BORDER_COLOR);
    }

    private void setValueFromMouse(double mouseX) {
        double raw = (mouseX - (this.getX() + 2.0)) / Math.max(1.0, this.getWidth() - 4.0);
        setValue(raw);
    }

    private void setValue(double value) {
        this.value = clamp(value);
        MenuMusicConfig.setVolume((float) this.value);
        updateMessage();
    }

    private void updateMessage() {
        this.setMessage(Text.literal("MUSIC " + Math.round(this.value * 100.0) + "%"));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
