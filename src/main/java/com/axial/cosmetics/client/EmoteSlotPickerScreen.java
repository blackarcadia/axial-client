package com.axial.cosmetics.client;

import com.axial.cosmetics.client.EmotePermissionState;
import io.github.kosmx.emotes.main.EmoteHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** A compact emote picker opened directly from an empty fast-wheel slot. */
public final class EmoteSlotPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int ROWS_PER_PAGE = 8;

    private final Screen parent;
    private final Consumer<EmoteHolder> onPick;
    private final List<EmoteHolder> emotes = new ArrayList<>();
    private int page;

    public EmoteSlotPickerScreen(Screen parent, Consumer<EmoteHolder> onPick) {
        super(Text.literal("ASSIGN EMOTE"));
        this.parent = parent;
        this.onPick = onPick;
        EmotePermissionState.filter(EmoteHolder.list).forEach(emotes::add);
        emotes.sort(Comparator.comparing(emote -> emote.name.getString(), String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    protected void init() {
        clearChildren();
        int panelWidth = Math.min(360, width - 24);
        int x = (width - panelWidth) / 2;
        int y = Math.max(28, (height - 254) / 2) + 45;
        int start = page * ROWS_PER_PAGE;

        for (int index = 0; index < ROWS_PER_PAGE && start + index < emotes.size(); index++) {
            EmoteHolder emote = emotes.get(start + index);
            addDrawableChild(ButtonWidget.builder(Text.literal(emote.name.getString()), button -> pick(emote))
                    .dimensions(x + 12, y + index * ROW_HEIGHT, panelWidth - 24, 20)
                    .build());
        }

        int footerY = y + ROWS_PER_PAGE * ROW_HEIGHT + 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> { page--; init(); })
                .dimensions(x + 12, footerY, 26, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> { page++; init(); })
                .dimensions(x + panelWidth - 38, footerY, 26, 20).build()).active = (page + 1) * ROWS_PER_PAGE < emotes.size();
        addDrawableChild(ButtonWidget.builder(Text.literal("CANCEL"), button -> close())
                .dimensions(x + panelWidth / 2 - 46, footerY, 92, 20).build());
    }

    private void pick(EmoteHolder emote) {
        onPick.accept(emote);
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuBackgroundRenderer.draw(context, this);
        int panelWidth = Math.min(360, width - 24);
        int panelHeight = 254;
        int x = (width - panelWidth) / 2;
        int y = Math.max(28, (height - panelHeight) / 2);
        context.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFF514065);
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xF510101A);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 13, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Click an emote to assign it to this wheel slot"),
                width / 2, y + 29, 0xFFB6ABEC);
        if (emotes.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("No emotes are available."), width / 2, y + 116, 0xFFF393AD);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
