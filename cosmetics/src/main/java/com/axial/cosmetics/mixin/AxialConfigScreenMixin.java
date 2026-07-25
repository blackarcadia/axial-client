package com.axial.cosmetics.mixin;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false)
public abstract class AxialConfigScreenMixin {
    private static final int UNIFIED_PANEL_WIDTH = 452;
    private static final int UNIFIED_PANEL_HEIGHT = 168;
    private static final int TILE_WIDTH = 132;
    private static final int COLOR_TILE_WIDTH = 203;
    private static final int ORIGINAL_TILE_WIDTH = 144;
    private static final int TILE_HEIGHT = 20;
    private static final int TILE_GAP = 10;
    private static final int PANEL_PADDING = 18;
    private static final int HEADER_HEIGHT = 30;
    private static final int COLOR_EDITOR_HEIGHT = 198;
    private static final int COLOR_EDITOR_GAP = 10;
    private static final Identifier MOVE_ARROW_ICON = AxialCosmetics.id("textures/gui/move-arrow.png");
    private static final Identifier OPTIONS_ICON = AxialCosmetics.id("textures/gui/options-icon.png");
    private static final Identifier BACK_ARROW_ICON = AxialCosmetics.id("textures/gui/back-arrow.png");
    private static Field axial_cosmetics$tilesField;
    private static Field axial_cosmetics$optionRowsField;
    private static Field axial_cosmetics$tileLabelField;
    private static Field axial_cosmetics$tileXField;
    private static Field axial_cosmetics$tileYField;
    private static Field axial_cosmetics$tileWidthField;
    private static Field axial_cosmetics$tileColorGroupField;
    private static Field axial_cosmetics$tileColorTargetField;
    private static Field axial_cosmetics$rowXField;
    private static Field axial_cosmetics$rowYField;
    private static Field axial_cosmetics$rowWidthField;
    private static Field axial_cosmetics$modeField;
    private static Field axial_cosmetics$panelWidthField;
    private static Field axial_cosmetics$panelHeightField;
    private static Field axial_cosmetics$panelHeightAnimatedField;
    private static Field axial_cosmetics$mainMenuContentHeightField;
    private static Field axial_cosmetics$mainMenuScrollOffsetField;
    private static Field axial_cosmetics$submenuContentHeightField;
    private static Field axial_cosmetics$submenuScrollOffsetField;
    private static Field axial_cosmetics$panelTargetYField;
    private static Field axial_cosmetics$activeColorEditorField;
    private static Field axial_cosmetics$activeColorEditorTopYField;
    private static Field axial_cosmetics$activeColorEditorGroupField;
    private static Field axial_cosmetics$activeColorEditorTargetField;
    private static Field axial_cosmetics$openAnimationField;
    private long axial_cosmetics$openedAtMs;
    private int axial_cosmetics$unifiedScrollOffset;

    @Inject(method = "method_25426", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$replaceSlideWithFadeInit(CallbackInfo ci) {
        axial_cosmetics$openedAtMs = System.currentTimeMillis();
        axial_cosmetics$finishSlideAnimation();
    }

    @Inject(method = "method_25394", at = @At("HEAD"), remap = false)
    private void axial_cosmetics$preventSlideAnimation(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        axial_cosmetics$finishSlideAnimation();
    }

    @Inject(method = "method_25394", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$drawSubtleOpenFade(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        long elapsedMs = System.currentTimeMillis() - axial_cosmetics$openedAtMs;
        float progress = Math.min(1f, Math.max(0f, elapsedMs / 95f));
        float eased = progress * progress * (3f - 2f * progress);
        int alpha = Math.round(24f * (1f - eased));
        if (alpha > 0) {
            try {
                int panelWidth = axial_cosmetics$getPanelWidth();
                int panelHeight = axial_cosmetics$getPanelHeight();
                int panelX = (((Screen) (Object) this).width - panelWidth) / 2;
                int panelY = (((Screen) (Object) this).height - panelHeight) / 2;
                context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, alpha << 24);
            } catch (ReflectiveOperationException ignored) {
                context.fill(0, 0, ((Screen) (Object) this).width, ((Screen) (Object) this).height, alpha << 24);
            }
        }
    }

    private void axial_cosmetics$finishSlideAnimation() {
        try {
            if (axial_cosmetics$openAnimationField == null) {
                axial_cosmetics$openAnimationField = this.getClass().getDeclaredField("openAnimation");
                axial_cosmetics$openAnimationField.setAccessible(true);
            }
            axial_cosmetics$openAnimationField.setFloat(this, 1f);
        } catch (ReflectiveOperationException ignored) {
            // Keep the base menu usable if axialutils changes its private animation field.
        }
    }

    @Inject(method = "visiblePanelOffset", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$removePanelSlide(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$normalizeMenuPanelSize(CallbackInfo ci) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || "MOVE".equals(mode.toString())) {
                return;
            }

            axial_cosmetics$setPanelWidth(UNIFIED_PANEL_WIDTH);
            axial_cosmetics$setPanelHeight(UNIFIED_PANEL_HEIGHT);
            axial_cosmetics$setPanelHeightAnimated(UNIFIED_PANEL_HEIGHT);
            axial_cosmetics$setPanelTargetY((((Screen) (Object) this).height - UNIFIED_PANEL_HEIGHT) / 2);

            if ("OPTIONS".equals(mode.toString())) {
                axial_cosmetics$normalizeOptionRows();
                int contentHeight = HEADER_HEIGHT + axial_cosmetics$getOptionRowsCount() * (TILE_HEIGHT + TILE_GAP) + PANEL_PADDING;
                axial_cosmetics$setSubmenuContentHeight(contentHeight);
                axial_cosmetics$syncUnifiedScrollOffset(false, axial_cosmetics$maxUnifiedScroll(contentHeight));
                return;
            }

            int contentHeight = axial_cosmetics$normalizeTiles();
            if ("MAIN".equals(mode.toString())) {
                axial_cosmetics$setMainMenuContentHeight(contentHeight);
                axial_cosmetics$syncUnifiedScrollOffset(true, axial_cosmetics$maxUnifiedScroll(contentHeight));
            } else {
                axial_cosmetics$setSubmenuContentHeight(contentHeight);
                axial_cosmetics$syncUnifiedScrollOffset(false, axial_cosmetics$maxUnifiedScroll(contentHeight));
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Keep the base menu usable if axialutils changes its private layout fields.
        }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$scrollUnifiedPanels(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || "MOVE".equals(mode.toString()) || verticalAmount == 0.0) {
                return;
            }

            int contentHeight = "MAIN".equals(mode.toString()) ? axial_cosmetics$getMainMenuContentHeight() : axial_cosmetics$getSubmenuContentHeight();
            int maxScroll = axial_cosmetics$maxUnifiedScroll(contentHeight);
            if (maxScroll <= 0) {
                return;
            }

            int scrollStep = 18;
            int nextScroll = axial_cosmetics$unifiedScrollOffset + (verticalAmount < 0.0 ? scrollStep : -scrollStep);
            boolean main = "MAIN".equals(mode.toString());
            axial_cosmetics$syncUnifiedScrollOffset(main, maxScroll, nextScroll);
            cir.setReturnValue(true);
        } catch (ReflectiveOperationException ignored) {
            // Fall through to the base scroll behavior if axialutils internals change.
        }
    }

    @Inject(method = "getCurrentScrollOffset", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$useUnifiedScrollOffset(CallbackInfoReturnable<Integer> cir) {
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode != null && !"MOVE".equals(mode.toString())) {
                cir.setReturnValue(axial_cosmetics$unifiedScrollOffset);
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to the base scroll offset if axialutils internals change.
        }
    }

    private int axial_cosmetics$maxUnifiedScroll(int contentHeight) {
        return Math.max(0, contentHeight - UNIFIED_PANEL_HEIGHT);
    }

    private void axial_cosmetics$syncUnifiedScrollOffset(boolean main, int maxScroll) throws ReflectiveOperationException {
        axial_cosmetics$syncUnifiedScrollOffset(main, maxScroll, axial_cosmetics$unifiedScrollOffset);
    }

    private void axial_cosmetics$syncUnifiedScrollOffset(boolean main, int maxScroll, int value) throws ReflectiveOperationException {
        axial_cosmetics$unifiedScrollOffset = Math.max(0, Math.min(value, maxScroll));
        if (main) {
            axial_cosmetics$setMainMenuScrollOffset(axial_cosmetics$unifiedScrollOffset, maxScroll);
        } else {
            axial_cosmetics$setSubmenuScrollOffset(axial_cosmetics$unifiedScrollOffset, maxScroll);
        }
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$restoreTitleOverlayButton(CallbackInfo ci) {
        if (axial_cosmetics$useUnifiedLayout()) {
            return;
        }
        try {
            if (axial_cosmetics$tilesField == null) {
                axial_cosmetics$tilesField = this.getClass().getDeclaredField("tiles");
                axial_cosmetics$tilesField.setAccessible(true);
            }

            Object rawTiles = axial_cosmetics$tilesField.get(this);
            if (!(rawTiles instanceof List<?> tiles)) {
                return;
            }

            Object scoreboardTile = null;
            Object titleOverlayTile = null;
            Object worldBorderTile = null;
            for (Object tile : tiles) {
                String label = axial_cosmetics$getTileLabel(tile);
                if ("SCOREBOARD".equals(label)) {
                    scoreboardTile = tile;
                } else if ("TITLE OVERLAY".equals(label)) {
                    titleOverlayTile = tile;
                } else if ("WORLD BORDER".equals(label)) {
                    worldBorderTile = tile;
                }
            }

            if (scoreboardTile == null || titleOverlayTile == null || worldBorderTile == null) {
                return;
            }

            int rowX = axial_cosmetics$getTileX(scoreboardTile) - 77;
            int rowY = axial_cosmetics$getTileY(scoreboardTile);
            axial_cosmetics$setTileBounds(scoreboardTile, rowX, rowY, 144);
            axial_cosmetics$setTileBounds(titleOverlayTile, rowX + 154, rowY, 144);
            axial_cosmetics$setTileBounds(worldBorderTile, rowX + 308, rowY, 144);
        } catch (ReflectiveOperationException ignored) {
            // Keep the base menu usable if axialutils changes its private layout fields.
        }
    }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false)
    private void axial_cosmetics$compactSubmenuTiles(CallbackInfo ci) {
        if (axial_cosmetics$useUnifiedLayout()) {
            return;
        }
        try {
            Object mode = axial_cosmetics$getMode();
            if (mode == null || "MAIN".equals(mode.toString()) || "MOVE".equals(mode.toString()) || "OPTIONS".equals(mode.toString())) {
                return;
            }
            if (axial_cosmetics$getActiveColorEditor() != null) {
                return;
            }

            Object rawTiles = axial_cosmetics$getTiles();
            if (!(rawTiles instanceof List<?> tiles) || tiles.isEmpty()) {
                return;
            }

            int panelWidth = axial_cosmetics$getPanelWidth();
            int panelX = (((Screen) (Object) this).width - panelWidth) / 2;
            int startX = panelX + 18;
            int leftX = startX;
            int rightX = startX + 154;
            int y = 30;
            int column = 0;

            for (Object tile : tiles) {
                int width = axial_cosmetics$getTileWidth(tile);
                if (width > 144) {
                    if (column != 0) {
                        y += 30;
                        column = 0;
                    }

                    axial_cosmetics$setTileBounds(tile, startX, y, panelWidth - 36);
                    y += 30;
                    continue;
                }

                axial_cosmetics$setTileBounds(tile, column == 0 ? leftX : rightX, y, 144);
                if (column == 0) {
                    column = 1;
                } else {
                    column = 0;
                    y += 30;
                }
            }

            int baseHeight = y + (column == 0 ? 18 : 48);
            int panelHeight = Math.min(((Screen) (Object) this).height - 32, Math.max(48, baseHeight));
            axial_cosmetics$setPanelHeight(panelHeight);
            axial_cosmetics$setSubmenuContentHeight(baseHeight);
            axial_cosmetics$clampSubmenuScrollOffset(Math.max(0, baseHeight - panelHeight));
            axial_cosmetics$setPanelTargetY((((Screen) (Object) this).height - panelHeight) / 2);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Keep the original axialutils layout if a private field changes.
        }
    }

    private static boolean axial_cosmetics$useUnifiedLayout() {
        return true;
    }

    private Object axial_cosmetics$getTiles() throws ReflectiveOperationException {
        if (axial_cosmetics$tilesField == null) {
            axial_cosmetics$tilesField = this.getClass().getDeclaredField("tiles");
            axial_cosmetics$tilesField.setAccessible(true);
        }
        return axial_cosmetics$tilesField.get(this);
    }

    private Object axial_cosmetics$getOptionRows() throws ReflectiveOperationException {
        if (axial_cosmetics$optionRowsField == null) {
            axial_cosmetics$optionRowsField = this.getClass().getDeclaredField("optionRows");
            axial_cosmetics$optionRowsField.setAccessible(true);
        }
        return axial_cosmetics$optionRowsField.get(this);
    }

    private Object axial_cosmetics$getMode() throws ReflectiveOperationException {
        if (axial_cosmetics$modeField == null) {
            axial_cosmetics$modeField = this.getClass().getDeclaredField("mode");
            axial_cosmetics$modeField.setAccessible(true);
        }
        return axial_cosmetics$modeField.get(this);
    }

    private Object axial_cosmetics$getActiveColorEditor() throws ReflectiveOperationException {
        if (axial_cosmetics$activeColorEditorField == null) {
            axial_cosmetics$activeColorEditorField = this.getClass().getDeclaredField("activeColorEditor");
            axial_cosmetics$activeColorEditorField.setAccessible(true);
        }
        return axial_cosmetics$activeColorEditorField.get(this);
    }

    private static String axial_cosmetics$getTileLabel(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileLabelField == null) {
            axial_cosmetics$tileLabelField = tile.getClass().getDeclaredField("label");
            axial_cosmetics$tileLabelField.setAccessible(true);
        }
        return (String) axial_cosmetics$tileLabelField.get(tile);
    }

    private static int axial_cosmetics$getTileX(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileXField == null) {
            axial_cosmetics$tileXField = tile.getClass().getDeclaredField("x");
            axial_cosmetics$tileXField.setAccessible(true);
        }
        return axial_cosmetics$tileXField.getInt(tile);
    }

    private static int axial_cosmetics$getTileY(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileYField == null) {
            axial_cosmetics$tileYField = tile.getClass().getDeclaredField("y");
            axial_cosmetics$tileYField.setAccessible(true);
        }
        return axial_cosmetics$tileYField.getInt(tile);
    }

    private static int axial_cosmetics$getTileWidth(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileWidthField == null) {
            axial_cosmetics$tileWidthField = tile.getClass().getDeclaredField("width");
            axial_cosmetics$tileWidthField.setAccessible(true);
        }
        return axial_cosmetics$tileWidthField.getInt(tile);
    }

    private static Object axial_cosmetics$getTileColorGroup(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileColorGroupField == null) {
            axial_cosmetics$tileColorGroupField = tile.getClass().getDeclaredField("colorGroup");
            axial_cosmetics$tileColorGroupField.setAccessible(true);
        }
        return axial_cosmetics$tileColorGroupField.get(tile);
    }

    private static Object axial_cosmetics$getTileColorTarget(Object tile) throws ReflectiveOperationException {
        if (axial_cosmetics$tileColorTargetField == null) {
            axial_cosmetics$tileColorTargetField = tile.getClass().getDeclaredField("colorTarget");
            axial_cosmetics$tileColorTargetField.setAccessible(true);
        }
        return axial_cosmetics$tileColorTargetField.get(tile);
    }

    private Object axial_cosmetics$getActiveColorEditorGroup(Object editor) throws ReflectiveOperationException {
        if (editor == null) {
            return null;
        }
        if (axial_cosmetics$activeColorEditorGroupField == null) {
            axial_cosmetics$activeColorEditorGroupField = editor.getClass().getDeclaredField("group");
            axial_cosmetics$activeColorEditorGroupField.setAccessible(true);
        }
        return axial_cosmetics$activeColorEditorGroupField.get(editor);
    }

    private Object axial_cosmetics$getActiveColorEditorTarget(Object editor) throws ReflectiveOperationException {
        if (editor == null) {
            return null;
        }
        if (axial_cosmetics$activeColorEditorTargetField == null) {
            axial_cosmetics$activeColorEditorTargetField = editor.getClass().getDeclaredField("target");
            axial_cosmetics$activeColorEditorTargetField.setAccessible(true);
        }
        return axial_cosmetics$activeColorEditorTargetField.get(editor);
    }

    private boolean axial_cosmetics$tileMatchesActiveColorEditor(Object tile, Object editor) throws ReflectiveOperationException {
        if (editor == null) {
            return false;
        }
        Object tileGroup = axial_cosmetics$getTileColorGroup(tile);
        Object tileTarget = axial_cosmetics$getTileColorTarget(tile);
        return tileGroup != null
                && tileGroup.equals(axial_cosmetics$getActiveColorEditorGroup(editor))
                && tileTarget != null
                && tileTarget.equals(axial_cosmetics$getActiveColorEditorTarget(editor));
    }

    private boolean axial_cosmetics$isColorTile(Object tile) throws ReflectiveOperationException {
        return axial_cosmetics$getTileColorGroup(tile) != null && axial_cosmetics$getTileColorTarget(tile) != null;
    }

    private void axial_cosmetics$setActiveColorEditorTopY(int value) throws ReflectiveOperationException {
        if (axial_cosmetics$activeColorEditorTopYField == null) {
            axial_cosmetics$activeColorEditorTopYField = this.getClass().getDeclaredField("activeColorEditorTopY");
            axial_cosmetics$activeColorEditorTopYField.setAccessible(true);
        }
        axial_cosmetics$activeColorEditorTopYField.setInt(this, value);
    }

    private static void axial_cosmetics$setTileBounds(Object tile, int x, int y, int width) throws ReflectiveOperationException {
        if (axial_cosmetics$tileXField == null) {
            axial_cosmetics$tileXField = tile.getClass().getDeclaredField("x");
            axial_cosmetics$tileXField.setAccessible(true);
        }
        if (axial_cosmetics$tileYField == null) {
            axial_cosmetics$tileYField = tile.getClass().getDeclaredField("y");
            axial_cosmetics$tileYField.setAccessible(true);
        }
        if (axial_cosmetics$tileWidthField == null) {
            axial_cosmetics$tileWidthField = tile.getClass().getDeclaredField("width");
            axial_cosmetics$tileWidthField.setAccessible(true);
        }
        axial_cosmetics$tileXField.setInt(tile, x);
        axial_cosmetics$tileYField.setInt(tile, y);
        axial_cosmetics$tileWidthField.setInt(tile, width);
    }

    private static void axial_cosmetics$setOptionRowBounds(Object row, int x, int y, int width) throws ReflectiveOperationException {
        if (axial_cosmetics$rowXField == null) {
            axial_cosmetics$rowXField = row.getClass().getDeclaredField("x");
            axial_cosmetics$rowXField.setAccessible(true);
        }
        if (axial_cosmetics$rowYField == null) {
            axial_cosmetics$rowYField = row.getClass().getDeclaredField("y");
            axial_cosmetics$rowYField.setAccessible(true);
        }
        if (axial_cosmetics$rowWidthField == null) {
            axial_cosmetics$rowWidthField = row.getClass().getDeclaredField("width");
            axial_cosmetics$rowWidthField.setAccessible(true);
        }
        axial_cosmetics$rowXField.setInt(row, x);
        axial_cosmetics$rowYField.setInt(row, y);
        axial_cosmetics$rowWidthField.setInt(row, width);
    }

    private int axial_cosmetics$normalizeTiles() throws ReflectiveOperationException {
        Object rawTiles = axial_cosmetics$getTiles();
        if (!(rawTiles instanceof List<?> tiles) || tiles.isEmpty()) {
            return HEADER_HEIGHT + PANEL_PADDING;
        }

        int panelX = (((Screen) (Object) this).width - UNIFIED_PANEL_WIDTH) / 2;
        int startX = panelX + PANEL_PADDING;
        int fullWidth = UNIFIED_PANEL_WIDTH - PANEL_PADDING * 2;
        int y = HEADER_HEIGHT;
        int column = 0;
        int columns = 3;
        int colorColumn = 0;
        boolean placeEditorAfterColorRow = false;
        Object activeEditor = axial_cosmetics$getActiveColorEditor();
        boolean placedEditor = false;

        for (Object tile : tiles) {
            boolean colorTile = axial_cosmetics$isColorTile(tile);
            int width = axial_cosmetics$getTileWidth(tile);
            String label = axial_cosmetics$getTileLabel(tile);
            boolean fullRow = !colorTile && width > ORIGINAL_TILE_WIDTH && !"TITLE OVERLAY".equals(label);
            if (fullRow) {
                if (column != 0) {
                    y += TILE_HEIGHT + TILE_GAP;
                    column = 0;
                }
                if (colorColumn != 0) {
                    y += TILE_HEIGHT + TILE_GAP;
                    if (placeEditorAfterColorRow && !placedEditor) {
                        axial_cosmetics$setActiveColorEditorTopY(y);
                        y += COLOR_EDITOR_HEIGHT + COLOR_EDITOR_GAP;
                        placedEditor = true;
                    }
                    colorColumn = 0;
                    placeEditorAfterColorRow = false;
                }
                axial_cosmetics$setTileBounds(tile, startX, y, fullWidth);
                y += TILE_HEIGHT + TILE_GAP;
                if (!placedEditor && axial_cosmetics$tileMatchesActiveColorEditor(tile, activeEditor)) {
                    axial_cosmetics$setActiveColorEditorTopY(y);
                    y += COLOR_EDITOR_HEIGHT + COLOR_EDITOR_GAP;
                    placedEditor = true;
                }
                continue;
            }

            if (colorTile) {
                if (column != 0) {
                    y += TILE_HEIGHT + TILE_GAP;
                    column = 0;
                }
                axial_cosmetics$setTileBounds(tile, startX + colorColumn * (COLOR_TILE_WIDTH + TILE_GAP), y, COLOR_TILE_WIDTH);
                placeEditorAfterColorRow = placeEditorAfterColorRow || axial_cosmetics$tileMatchesActiveColorEditor(tile, activeEditor);
                colorColumn++;
                if (colorColumn >= 2) {
                    y += TILE_HEIGHT + TILE_GAP;
                    if (placeEditorAfterColorRow && !placedEditor) {
                        axial_cosmetics$setActiveColorEditorTopY(y);
                        y += COLOR_EDITOR_HEIGHT + COLOR_EDITOR_GAP;
                        placedEditor = true;
                    }
                    colorColumn = 0;
                    placeEditorAfterColorRow = false;
                }
                continue;
            }

            if (colorColumn != 0) {
                y += TILE_HEIGHT + TILE_GAP;
                if (placeEditorAfterColorRow && !placedEditor) {
                    axial_cosmetics$setActiveColorEditorTopY(y);
                    y += COLOR_EDITOR_HEIGHT + COLOR_EDITOR_GAP;
                    placedEditor = true;
                }
                colorColumn = 0;
                placeEditorAfterColorRow = false;
            }

            axial_cosmetics$setTileBounds(tile, startX + column * (TILE_WIDTH + TILE_GAP), y, TILE_WIDTH);
            column++;
            if (column >= columns) {
                column = 0;
                y += TILE_HEIGHT + TILE_GAP;
            }
        }

        if (colorColumn != 0) {
            y += TILE_HEIGHT + TILE_GAP;
            if (placeEditorAfterColorRow && !placedEditor) {
                axial_cosmetics$setActiveColorEditorTopY(y);
                y += COLOR_EDITOR_HEIGHT + COLOR_EDITOR_GAP;
            }
        }

        return y + (column == 0 ? PANEL_PADDING : TILE_HEIGHT + PANEL_PADDING);
    }

    private void axial_cosmetics$normalizeOptionRows() throws ReflectiveOperationException {
        Object rawRows = axial_cosmetics$getOptionRows();
        if (!(rawRows instanceof List<?> rows)) {
            return;
        }

        int panelX = (((Screen) (Object) this).width - UNIFIED_PANEL_WIDTH) / 2;
        int rowX = panelX + PANEL_PADDING;
        int rowWidth = UNIFIED_PANEL_WIDTH - PANEL_PADDING * 2;
        int y = HEADER_HEIGHT;
        for (Object row : rows) {
            axial_cosmetics$setOptionRowBounds(row, rowX, y, rowWidth);
            y += TILE_HEIGHT + TILE_GAP;
        }
    }

    private int axial_cosmetics$getOptionRowsCount() throws ReflectiveOperationException {
        Object rawRows = axial_cosmetics$getOptionRows();
        return rawRows instanceof List<?> rows ? rows.size() : 0;
    }

    private int axial_cosmetics$getPanelWidth() throws ReflectiveOperationException {
        if (axial_cosmetics$panelWidthField == null) {
            axial_cosmetics$panelWidthField = this.getClass().getDeclaredField("panelWidth");
            axial_cosmetics$panelWidthField.setAccessible(true);
        }
        return axial_cosmetics$panelWidthField.getInt(this);
    }

    private void axial_cosmetics$setPanelWidth(int value) throws ReflectiveOperationException {
        if (axial_cosmetics$panelWidthField == null) {
            axial_cosmetics$panelWidthField = this.getClass().getDeclaredField("panelWidth");
            axial_cosmetics$panelWidthField.setAccessible(true);
        }
        axial_cosmetics$panelWidthField.setInt(this, value);
    }

    private void axial_cosmetics$setPanelHeight(int value) throws ReflectiveOperationException {
        if (axial_cosmetics$panelHeightField == null) {
            axial_cosmetics$panelHeightField = this.getClass().getDeclaredField("panelHeight");
            axial_cosmetics$panelHeightField.setAccessible(true);
        }
        axial_cosmetics$panelHeightField.setInt(this, value);
    }

    private int axial_cosmetics$getPanelHeight() throws ReflectiveOperationException {
        if (axial_cosmetics$panelHeightField == null) {
            axial_cosmetics$panelHeightField = this.getClass().getDeclaredField("panelHeight");
            axial_cosmetics$panelHeightField.setAccessible(true);
        }
        return axial_cosmetics$panelHeightField.getInt(this);
    }

    private void axial_cosmetics$setPanelHeightAnimated(float value) throws ReflectiveOperationException {
        if (axial_cosmetics$panelHeightAnimatedField == null) {
            axial_cosmetics$panelHeightAnimatedField = this.getClass().getDeclaredField("panelHeightAnimated");
            axial_cosmetics$panelHeightAnimatedField.setAccessible(true);
        }
        axial_cosmetics$panelHeightAnimatedField.setFloat(this, value);
    }

    private void axial_cosmetics$setMainMenuContentHeight(int value) throws ReflectiveOperationException {
        if (axial_cosmetics$mainMenuContentHeightField == null) {
            axial_cosmetics$mainMenuContentHeightField = this.getClass().getDeclaredField("mainMenuContentHeight");
            axial_cosmetics$mainMenuContentHeightField.setAccessible(true);
        }
        axial_cosmetics$mainMenuContentHeightField.setInt(this, value);
    }

    private void axial_cosmetics$clampMainMenuScrollOffset(int maxValue) throws ReflectiveOperationException {
        if (axial_cosmetics$mainMenuScrollOffsetField == null) {
            axial_cosmetics$mainMenuScrollOffsetField = this.getClass().getDeclaredField("mainMenuScrollOffset");
            axial_cosmetics$mainMenuScrollOffsetField.setAccessible(true);
        }
        int value = axial_cosmetics$mainMenuScrollOffsetField.getInt(this);
        axial_cosmetics$mainMenuScrollOffsetField.setInt(this, Math.max(0, Math.min(value, maxValue)));
    }

    private int axial_cosmetics$getMainMenuContentHeight() throws ReflectiveOperationException {
        if (axial_cosmetics$mainMenuContentHeightField == null) {
            axial_cosmetics$mainMenuContentHeightField = this.getClass().getDeclaredField("mainMenuContentHeight");
            axial_cosmetics$mainMenuContentHeightField.setAccessible(true);
        }
        return axial_cosmetics$mainMenuContentHeightField.getInt(this);
    }

    private int axial_cosmetics$getMainMenuScrollOffset() throws ReflectiveOperationException {
        if (axial_cosmetics$mainMenuScrollOffsetField == null) {
            axial_cosmetics$mainMenuScrollOffsetField = this.getClass().getDeclaredField("mainMenuScrollOffset");
            axial_cosmetics$mainMenuScrollOffsetField.setAccessible(true);
        }
        return axial_cosmetics$mainMenuScrollOffsetField.getInt(this);
    }

    private void axial_cosmetics$setMainMenuScrollOffset(int value, int maxValue) throws ReflectiveOperationException {
        if (axial_cosmetics$mainMenuScrollOffsetField == null) {
            axial_cosmetics$mainMenuScrollOffsetField = this.getClass().getDeclaredField("mainMenuScrollOffset");
            axial_cosmetics$mainMenuScrollOffsetField.setAccessible(true);
        }
        axial_cosmetics$mainMenuScrollOffsetField.setInt(this, Math.max(0, Math.min(value, maxValue)));
    }

    private void axial_cosmetics$setSubmenuContentHeight(int value) throws ReflectiveOperationException {
        if (axial_cosmetics$submenuContentHeightField == null) {
            axial_cosmetics$submenuContentHeightField = this.getClass().getDeclaredField("submenuContentHeight");
            axial_cosmetics$submenuContentHeightField.setAccessible(true);
        }
        axial_cosmetics$submenuContentHeightField.setInt(this, value);
    }

    private int axial_cosmetics$getSubmenuContentHeight() throws ReflectiveOperationException {
        if (axial_cosmetics$submenuContentHeightField == null) {
            axial_cosmetics$submenuContentHeightField = this.getClass().getDeclaredField("submenuContentHeight");
            axial_cosmetics$submenuContentHeightField.setAccessible(true);
        }
        return axial_cosmetics$submenuContentHeightField.getInt(this);
    }

    private int axial_cosmetics$getSubmenuScrollOffset() throws ReflectiveOperationException {
        if (axial_cosmetics$submenuScrollOffsetField == null) {
            axial_cosmetics$submenuScrollOffsetField = this.getClass().getDeclaredField("submenuScrollOffset");
            axial_cosmetics$submenuScrollOffsetField.setAccessible(true);
        }
        return axial_cosmetics$submenuScrollOffsetField.getInt(this);
    }

    private void axial_cosmetics$setSubmenuScrollOffset(int value, int maxValue) throws ReflectiveOperationException {
        if (axial_cosmetics$submenuScrollOffsetField == null) {
            axial_cosmetics$submenuScrollOffsetField = this.getClass().getDeclaredField("submenuScrollOffset");
            axial_cosmetics$submenuScrollOffsetField.setAccessible(true);
        }
        axial_cosmetics$submenuScrollOffsetField.setInt(this, Math.max(0, Math.min(value, maxValue)));
    }

    private void axial_cosmetics$clampSubmenuScrollOffset(int maxValue) throws ReflectiveOperationException {
        if (axial_cosmetics$submenuScrollOffsetField == null) {
            axial_cosmetics$submenuScrollOffsetField = this.getClass().getDeclaredField("submenuScrollOffset");
            axial_cosmetics$submenuScrollOffsetField.setAccessible(true);
        }
        int value = axial_cosmetics$submenuScrollOffsetField.getInt(this);
        axial_cosmetics$submenuScrollOffsetField.setInt(this, Math.max(0, Math.min(value, maxValue)));
    }

    private void axial_cosmetics$setPanelTargetY(int value) throws ReflectiveOperationException {
        if (axial_cosmetics$panelTargetYField == null) {
            axial_cosmetics$panelTargetYField = this.getClass().getDeclaredField("panelTargetY");
            axial_cosmetics$panelTargetYField.setAccessible(true);
        }
        axial_cosmetics$panelTargetYField.setInt(this, value);
    }

    @Inject(method = "drawNavIcon", at = @At("HEAD"), cancellable = true, remap = false)
    private void axial_cosmetics$drawCustomNavIcon(DrawContext context, int x, int y, int width, int height, @Coerce Object tab, CallbackInfo ci) {
        Identifier icon = switch (tab == null ? "" : tab.toString()) {
            case "MOVE" -> MOVE_ARROW_ICON;
            case "OPTIONS" -> OPTIONS_ICON;
            default -> null;
        };

        if (icon == null) {
            return;
        }

        int iconSize = 16;
        int iconX = x + (width - iconSize) / 2;
        int iconY = y + (height - iconSize) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0f, 0.0f, iconSize, iconSize, 64, 64, 64, 64);
        ci.cancel();
    }

    @Inject(
            method = "drawBackButton",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25294(IIIII)V", ordinal = 0),
            cancellable = true,
            remap = false
    )
    private void axial_cosmetics$drawBackArrowIcon(DrawContext context, int mouseX, int mouseY, int panelX, int panelY, CallbackInfo ci) {
        int buttonX = panelX + 18;
        int buttonY = panelY + 6;

        int iconWidth = 16;
        int iconHeight = 13;
        int iconX = buttonX + (24 - iconWidth) / 2 - 4;
        int iconY = buttonY + (18 - iconHeight) / 2 - 1;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, BACK_ARROW_ICON, iconX, iconY, 0.0f, 0.0f, iconWidth, iconHeight, 64, 64, 64, 64);
        ci.cancel();
    }
}
