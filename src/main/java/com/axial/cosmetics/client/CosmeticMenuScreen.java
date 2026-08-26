package com.axial.cosmetics.client;

import com.axial.cosmetics.data.CosmeticManager;
import com.axial.cosmetics.data.CosmeticTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CosmeticMenuScreen extends Screen {
    private final CosmeticManager manager;
    private final MinecraftClient clientRef;
    private final List<CheckboxWidget> checkboxes = new ArrayList<>();

    public CosmeticMenuScreen(CosmeticManager manager, MinecraftClient client) {
        super(Text.literal("Cosmetics"));
        this.manager = manager;
        this.clientRef = client;
    }

    @Override
    protected void init() {
        checkboxes.clear();
        int y = 40;
        if (clientRef.player == null) {
            return;
        }

        for (CosmeticTexture texture : manager.get(clientRef.player.getUuid(), clientRef.player.getName().getString())) {
            String id = texture.definition().id();
            boolean enabled = manager.isEnabled(id);
            CheckboxWidget box = CheckboxWidget.builder(Text.literal(id), textRenderer)
                    .pos(this.width / 2 - 100, y)
                    .checked(enabled)
                    .callback((cb, checked) -> manager.setEnabled(id, checked, client))
                    .build();
            addDrawableChild(box);
            checkboxes.add(box);
            y += 24;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(this.width / 2 - 40, this.height - 40, 80, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, "Cosmetics", this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuBackgroundRenderer.draw(context, this);
    }
}
