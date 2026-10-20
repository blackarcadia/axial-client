package com.axial.cosmetics.mixin;

import io.github.kosmx.emotes.arch.gui.widgets.EmoteListWidget;
import io.github.kosmx.emotes.arch.gui.widgets.PlayerPreview;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Arranges Emotecraft's existing controls into Axial's compact loadout dashboard. */
@Mixin(targets = "io.github.kosmx.emotes.arch.screen.EmoteMenu", remap = false)
public abstract class EmoteMenuDashboardMixin extends Screen {
    @Shadow protected EmoteListWidget list;
    @Shadow protected PlayerPreview preview;
    @Shadow protected TextFieldWidget searchBox;
    @Shadow protected AbstractFastChooseWidget fastChoose;
    @Shadow private ButtonWidget setKeyButton;
    @Shadow private ButtonWidget resetButton;

    protected EmoteMenuDashboardMixin() {
        super(Text.empty());
    }

    @Inject(method = "method_48640", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$arrangeLoadout(CallbackInfo ci) {
        if (list == null || preview == null || fastChoose == null || searchBox == null) return;

        int margin = Math.max(24, width / 18);
        int gap = Math.max(12, width / 110);
        int top = Math.max(76, height / 11);
        int footerY = height - Math.max(48, height / 19);
        int contentHeight = Math.max(210, footerY - top - 18);
        int leftWidth = Math.max(270, width * 31 / 100);
        int centerWidth = Math.max(250, width * 22 / 100);
        int rightX = margin + leftWidth + centerWidth + gap * 2;
        int rightWidth = Math.max(260, width - rightX - margin);
        int centerX = margin + leftWidth + gap;

        list.position(margin, top, leftWidth, contentHeight);
        preview.setDimensions(centerWidth, contentHeight);
        preview.setPosition(centerX, top);

        int searchWidth = Math.min(centerWidth + rightWidth, Math.max(280, width * 30 / 100));
        searchBox.setDimensions(searchWidth, 20);
        searchBox.setPosition(centerX + (centerWidth + rightWidth - searchWidth) / 2, Math.max(24, top - 42));

        int wheelSize = Math.min(Math.min(rightWidth - 24, contentHeight - 72), Math.max(220, width / 5));
        fastChoose.setSize(wheelSize);
        fastChoose.setPosition(rightX + (rightWidth - wheelSize) / 2, top + 56 + (contentHeight - 56 - wheelSize) / 2);

        int assignmentWidth = Math.max(120, (rightWidth - 26) * 2 / 3);
        setKeyButton.setDimensions(assignmentWidth, 20);
        setKeyButton.setPosition(rightX + 12, top + 27);
        resetButton.setDimensions(rightWidth - assignmentWidth - 26, 20);
        resetButton.setPosition(rightX + assignmentWidth + 14, top + 27);

        arrangeFooterButtons(margin, footerY, width - margin * 2, gap);
    }

    private void arrangeFooterButtons(int x, int y, int availableWidth, int gap) {
        int buttonWidth = Math.max(120, (availableWidth - gap * 2) / 3);
        int index = 0;
        for (Element element : children()) {
            if (!(element instanceof ClickableWidget button)) continue;
            String label = button.getMessage().getString();
            if (!(label.equalsIgnoreCase("Open Emotes Folder")
                    || label.equalsIgnoreCase("Done")
                    || label.equalsIgnoreCase("Mod Options"))) continue;
            button.setDimensions(buttonWidth, 20);
            button.setPosition(x + index * (buttonWidth + gap), y);
            index++;
        }
    }

}
