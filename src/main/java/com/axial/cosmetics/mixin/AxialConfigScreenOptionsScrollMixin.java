package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenOptionsScrollMixin extends Screen {
    @Shadow @Final private List<?> optionRows;
    @Shadow private int panelWidth;
    @Shadow private int panelHeight;
    @Shadow private int submenuContentHeight;
    @Shadow private int submenuScrollOffset;
    @Shadow private int currentPanelBaseY() { throw new AssertionError(); }

    @Unique private static Field axial_cosmetics$modeField;
    @Unique private static Field axial_cosmetics$rowYField;
    @Unique private int axial_cosmetics$pendingScroll;

    protected AxialConfigScreenOptionsScrollMixin(Text title) {
        super(title);
    }

    @Inject(method = "rebuildLayout", at = @At("HEAD"), remap = false, order = 2000)
    private void axial_cosmetics$rememberOptionsScroll(CallbackInfo ci) {
        if (axial_cosmetics$isOptions()) axial_cosmetics$pendingScroll = submenuScrollOffset;
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 2000)
    private void axial_cosmetics$restoreAndApplyOptionsScroll(CallbackInfo ci) {
        if (!axial_cosmetics$isOptions()) return;

        panelHeight = Math.min(height - 32, Math.max(48, submenuContentHeight));
        submenuScrollOffset = clamp(axial_cosmetics$pendingScroll, 0, maxScroll());
        for (Object row : optionRows) {
            try {
                Field y = axial_cosmetics$rowY(row);
                y.setInt(row, y.getInt(row) - submenuScrollOffset);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Could not scroll Axial options rows", ex);
            }
        }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$scrollOptions(double mouseX, double mouseY, double horizontalAmount,
            double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!axial_cosmetics$isOptions() || verticalAmount == 0.0 || maxScroll() == 0) return;
        submenuScrollOffset = clamp(submenuScrollOffset + (verticalAmount < 0.0 ? 15 : -15), 0, maxScroll());
        cir.setReturnValue(true);
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 2000)
    private void axial_cosmetics$drawOptionsScrollBar(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        if (!axial_cosmetics$isOptions() || maxScroll() == 0) return;

        int panelX = (width - panelWidth) / 2;
        int viewportTop = currentPanelBaseY() + 30;
        int viewportHeight = panelHeight - 48;
        int contentHeight = Math.max(viewportHeight + 1, submenuContentHeight - 48);
        int thumbHeight = Math.max(18, Math.round(viewportHeight * (viewportHeight / (float) contentHeight)));
        int thumbTravel = Math.max(1, viewportHeight - thumbHeight);
        int thumbY = viewportTop + Math.round(submenuScrollOffset / (float) maxScroll() * thumbTravel);
        int trackX = panelX + panelWidth - 12;
        context.fill(trackX, viewportTop, trackX + 4, viewportTop + viewportHeight, 0x2AFFFFFF);
        context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xA0B0B5CF);
    }

    @Unique
    private boolean axial_cosmetics$isOptions() {
        try {
            if (axial_cosmetics$modeField == null) {
                axial_cosmetics$modeField = getClass().getDeclaredField("mode");
                axial_cosmetics$modeField.setAccessible(true);
            }
            Object mode = axial_cosmetics$modeField.get(this);
            return mode != null && "OPTIONS".equals(mode.toString());
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    @Unique
    private static Field axial_cosmetics$rowY(Object row) {
        if (axial_cosmetics$rowYField == null) {
            try {
                axial_cosmetics$rowYField = row.getClass().getDeclaredField("y");
                axial_cosmetics$rowYField.setAccessible(true);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Could not access Axial options row position", ex);
            }
        }
        return axial_cosmetics$rowYField;
    }

    @Unique private int maxScroll() { return Math.max(0, submenuContentHeight - panelHeight); }
    @Unique private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
