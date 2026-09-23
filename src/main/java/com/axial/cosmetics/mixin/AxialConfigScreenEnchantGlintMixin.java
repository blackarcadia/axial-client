package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.CrosshairColorPickerScreen;
import com.axial.cosmetics.client.EnchantGlintConfig;
import com.axial.cosmetics.client.EnchantGlintSliderWidget;
import com.axial.cosmetics.client.ThirdPersonNameTagsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenEnchantGlintMixin extends Screen {
    @Unique private static final StyleSpriteSource.Font AXIAL_GLINT_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    @Unique private static final int AXIAL_GLINT_HEADER_HEIGHT = 20;
    @Shadow @Final private List<?> optionRows;
    @Shadow private int panelHeight;
    @Shadow private int panelWidth;
    @Shadow private int panelTargetY;
    @Shadow private int submenuContentHeight;
    @Shadow private int currentPanelBaseY() { throw new AssertionError(); }

    @Unique private EnchantGlintSliderWidget axial_cosmetics$glintSlider;
    @Unique private ButtonWidget axial_cosmetics$thirdPersonNameTags;
    @Unique private ButtonWidget axial_cosmetics$glintColor;
    @Unique private ButtonWidget axial_cosmetics$glintReset;
    @Unique private int axial_cosmetics$glintY;
    @Unique private boolean axial_cosmetics$glintOptions;

    protected AxialConfigScreenEnchantGlintMixin(Text title) { super(title); }

    @Inject(method = "method_25426", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$initGlintControls(CallbackInfo ci) {
        axial_cosmetics$glintSlider = addSelectableChild(new EnchantGlintSliderWidget());
        axial_cosmetics$thirdPersonNameTags = addSelectableChild(ButtonWidget.builder(axial_cosmetics$thirdPersonNameTagsText(), button -> {
                    ThirdPersonNameTagsConfig.toggle();
                    button.setMessage(axial_cosmetics$thirdPersonNameTagsText());
                })
                .dimensions(0, 0, 392, 20).build());
        axial_cosmetics$glintColor = addSelectableChild(ButtonWidget.builder(axial_cosmetics$glintText("ENCHANT GLINT COLOR"), button ->
                MinecraftClient.getInstance().setScreen(new CrosshairColorPickerScreen(this, "ENCHANT GLINT",
                        EnchantGlintConfig.color(), EnchantGlintConfig::setColor, EnchantGlintConfig::save)))
                .dimensions(0, 0, 308, 20).build());
        axial_cosmetics$glintReset = addSelectableChild(ButtonWidget.builder(axial_cosmetics$glintText("DEFAULT"), button ->
                EnchantGlintConfig.resetColor()).dimensions(0, 0, 76, 20).build());
        axial_cosmetics$positionGlintControls();
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$extendOptions(CallbackInfo ci) {
        axial_cosmetics$glintOptions = false;
        for (Object row : optionRows) {
            try {
                Field label = row.getClass().getDeclaredField("label");
                label.setAccessible(true);
                if (!"PLAYER SHADOWS".equals(label.get(row))) continue;
                Field y = row.getClass().getDeclaredField("y");
                y.setAccessible(true);
                axial_cosmetics$glintY = y.getInt(row) + 28;
                axial_cosmetics$glintOptions = true;
                submenuContentHeight = axial_cosmetics$glintY + 158;
                panelHeight = Math.min(height - 32, submenuContentHeight);
                panelTargetY = (height - panelHeight) / 2;
                break;
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Could not place enchant glint options below Player Shadows", ex);
            }
        }
        axial_cosmetics$positionGlintControls();
    }

    @Unique
    private void axial_cosmetics$positionGlintControls() {
        if (axial_cosmetics$glintSlider == null) return;
        int x = (width - panelWidth) / 2 + 14;
        int y = currentPanelBaseY() + axial_cosmetics$glintY;
        axial_cosmetics$thirdPersonNameTags.setPosition(x, y);
        y += 28 + AXIAL_GLINT_HEADER_HEIGHT;
        axial_cosmetics$glintSlider.setPosition(x, y);
        axial_cosmetics$glintColor.setPosition(x, y + 28);
        axial_cosmetics$glintReset.setPosition(x + 316, y + 28);
        axial_cosmetics$glintSlider.visible = axial_cosmetics$glintOptions;
        axial_cosmetics$thirdPersonNameTags.visible = axial_cosmetics$glintOptions;
        axial_cosmetics$glintColor.visible = axial_cosmetics$glintOptions;
        axial_cosmetics$glintReset.visible = axial_cosmetics$glintOptions;
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false)
    private void axial_cosmetics$drawGlintControls(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        axial_cosmetics$positionGlintControls();
        if (!axial_cosmetics$glintOptions || axial_cosmetics$glintSlider == null) return;
        int headerX = axial_cosmetics$glintSlider.getX();
        axial_cosmetics$thirdPersonNameTags.render(context, mouseX, mouseY, delta);
        int headerY = axial_cosmetics$glintSlider.getY() - AXIAL_GLINT_HEADER_HEIGHT;
        Text header = axial_cosmetics$glintText("ENCHANT GLINT");
        context.drawTextWithShadow(textRenderer, header, headerX + 2, headerY + 2, 0xFFC6D0F3);
        context.fill(headerX + textRenderer.getWidth(header) + 10, headerY + 6,
                headerX + panelWidth - 28, headerY + 7, 0x998F5DFF);
        axial_cosmetics$glintSlider.render(context, mouseX, mouseY, delta);
        axial_cosmetics$glintColor.render(context, mouseX, mouseY, delta);
        axial_cosmetics$glintReset.render(context, mouseX, mouseY, delta);
        int x = axial_cosmetics$glintColor.getX() + 8;
        int y = axial_cosmetics$glintColor.getY() + 6;
        context.fill(x, y, x + 8, y + 8, EnchantGlintConfig.color());
    }

    @Unique
    private static Text axial_cosmetics$glintText(String value) {
        return Text.literal(value).styled(style -> style.withFont(AXIAL_GLINT_FONT));
    }

    @Unique
    private static Text axial_cosmetics$thirdPersonNameTagsText() {
        return axial_cosmetics$glintText("THIRD PERSON NAME TAGS: "
                + (ThirdPersonNameTagsConfig.enabled() ? "ON" : "OFF"));
    }

}
