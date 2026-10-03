package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.InformationHudExtrasConfig;
import com.axial.cosmetics.client.PotionsHudSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.axial.axialutils.client.AxialConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Sectioned, scrollable Information HUD settings. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1500)
public abstract class AxialConfigScreenInformationHudOptionsMixin {
    @Unique private static final int WIDTH = 452, HEIGHT = 168, COLORS_Y = 198, CONTENT_HEIGHT = 418;
    @Unique private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    @Unique private int axial_cosmetics$scroll;

    @Inject(method = "rebuildLayout", at = @At("HEAD"), remap = false, order = 3000)
    private void rememberScroll(CallbackInfo ci) { if (isHud()) axial_cosmetics$scroll = getInt("submenuScrollOffset"); }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 3000)
    private void buildHud(CallbackInfo ci) {
        if (!isHud()) { removeBox(); return; }
        try {
            List<Object> tiles = list("tiles"), rows = list("optionRows");
            tiles.clear(); rows.clear();
            int panelX = (((Screen) (Object) this).width - WIDTH) / 2;
            addEnabledTile(tiles, panelX);
            addOption(panelX, 84, 0, "PING", InformationHudExtrasConfig::showPing, InformationHudExtrasConfig::setPing);
            addOption(panelX, 84, 1, "COORDINATES", InformationHudExtrasConfig::showCoordinates, InformationHudExtrasConfig::setCoordinates);
            addOption(panelX, 114, 0, "CHARGE PER MIN", () -> AxialConfigManager.get().showPickaxeChargePerMinute, v -> { AxialConfigManager.get().showPickaxeChargePerMinute = v; AxialConfigManager.save(); });
            addOption(panelX, 114, 1, "XP PER MIN", () -> AxialConfigManager.get().showMiningXpPerMinute, v -> { AxialConfigManager.get().showMiningXpPerMinute = v; AxialConfigManager.save(); });
            addOption(panelX, 144, 0, "SERVER", InformationHudExtrasConfig::showServer, InformationHudExtrasConfig::setServer);
            addOption(panelX, 144, 1, "FACING", InformationHudExtrasConfig::showFacing, InformationHudExtrasConfig::setFacing);
            addColor(panelX, COLORS_Y, "TITLE COLOR", () -> AxialConfigManager.get().informationHudTitleColor, v -> { AxialConfigManager.get().informationHudTitleColor = v; AxialConfigManager.save(); });
            addColor(panelX, COLORS_Y + 30, "PING COLOR", InformationHudExtrasConfig::pingColor, InformationHudExtrasConfig::setPingColor);
            addColor(panelX, COLORS_Y + 60, "COORDINATES COLOR", InformationHudExtrasConfig::coordinatesColor, InformationHudExtrasConfig::setCoordinatesColor);
            addColor(panelX, COLORS_Y + 90, "CHARGE / MIN COLOR", () -> AxialConfigManager.get().informationHudChargeColor, v -> { AxialConfigManager.get().informationHudChargeColor = v; AxialConfigManager.save(); });
            addColor(panelX, COLORS_Y + 120, "XP / MIN COLOR", () -> AxialConfigManager.get().informationHudXpColor, v -> { AxialConfigManager.get().informationHudXpColor = v; AxialConfigManager.save(); });
            addColor(panelX, COLORS_Y + 150, "SERVER COLOR", InformationHudExtrasConfig::serverColor, InformationHudExtrasConfig::setServerColor);
            addColor(panelX, COLORS_Y + 180, "FACING COLOR", InformationHudExtrasConfig::facingColor, InformationHudExtrasConfig::setFacingColor);
            setInt("panelWidth", WIDTH); setInt("panelHeight", Math.min(((Screen) (Object) this).height - 32, HEIGHT));
            setInt("panelHeightAnimated", getInt("panelHeight")); setInt("submenuContentHeight", CONTENT_HEIGHT);
            setInt("panelTargetY", (((Screen) (Object) this).height - getInt("panelHeight")) / 2);
            axial_cosmetics$scroll = clamp(axial_cosmetics$scroll, 0, maxScroll()); setInt("submenuScrollOffset", axial_cosmetics$scroll);
            shift(-axial_cosmetics$scroll);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not build Information HUD settings", e); }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void scrollHud(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (!isHud() || vertical == 0 || maxScroll() == 0) return;
        int next = clamp(axial_cosmetics$scroll + (vertical < 0 ? 18 : -18), 0, maxScroll());
        try { shift(axial_cosmetics$scroll - next); axial_cosmetics$scroll = next; setInt("submenuScrollOffset", next); cir.setReturnValue(true); }
        catch (ReflectiveOperationException ignored) { }
    }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 3000)
    private void sections(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isHud()) return;
        int panelX = (((Screen) (Object) this).width - getInt("panelWidth")) / 2, panelY = (((Screen) (Object) this).height - getInt("panelHeight")) / 2;
        context.enableScissor(panelX, panelY + 26, panelX + getInt("panelWidth"), panelY + getInt("panelHeight") - 14);
        try {
            renderOptionRows(context, mouseX, mouseY, panelY);
            divider(context, panelX + 18, panelY + 58 - axial_cosmetics$scroll, "HUD OPTIONS");
            divider(context, panelX + 18, panelY + 174 - axial_cosmetics$scroll, "COLOR");
            drawScrollBar(context, panelX, panelY);
        }
        finally { context.disableScissor(); }
    }

    @Inject(method = "drawScrollBar", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressSharedHudScrollBar(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (isHud()) ci.cancel();
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void clickOptionRow(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!isHud() || click.button() != 0) return;
        try {
            int panelY = (((Screen) (Object) this).height - getInt("panelHeight")) / 2;
            for (Object row : list("optionRows")) {
                if (!insideVisibleControl(row, click.x(), click.y(), panelY)) continue;
                Method contains = row.getClass().getDeclaredMethod("contains", double.class, double.class, int.class);
                contains.setAccessible(true);
                if (!(Boolean) contains.invoke(row, click.x(), click.y(), panelY)) continue;
                Method activate = row.getClass().getDeclaredMethod("activate");
                activate.setAccessible(true);
                activate.invoke(row);
                cir.setReturnValue(true);
                return;
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Unique private void addOption(int panelX, int y, int column, String label, BooleanSupplier get, Consumer<Boolean> set) throws ReflectiveOperationException {
        Class<?> s = getClass(), a = Class.forName(s.getName() + "$BooleanSupplier"), b = Class.forName(s.getName() + "$BooleanConsumer");
        Object getter = Proxy.newProxyInstance(a.getClassLoader(), new Class[]{a}, (p,m,args) -> get.getAsBoolean());
        Object setter = Proxy.newProxyInstance(b.getClassLoader(), new Class[]{b}, (p,m,args) -> { set.accept((Boolean) args[0]); return null; });
        Method add = s.getDeclaredMethod("addOptionRow", int.class, int.class, String.class, a, b); add.setAccessible(true);
        add.invoke(this, panelX + 28 + column * 212, y, label, getter, setter);
        Field width = list("optionRows").getLast().getClass().getDeclaredField("width");
        width.setAccessible(true);
        width.setInt(list("optionRows").getLast(), 170);
    }

    @Unique private void addEnabledTile(List<Object> tiles, int panelX) throws ReflectiveOperationException {
        Class<?> s = getClass(), supplierType = Class.forName(s.getName() + "$BooleanSupplier"), consumerType = Class.forName(s.getName() + "$BooleanConsumer");
        Object getter = Proxy.newProxyInstance(supplierType.getClassLoader(), new Class[]{supplierType},
                (p, m, args) -> AxialConfigManager.get().hudEnabled);
        Object setter = Proxy.newProxyInstance(consumerType.getClassLoader(), new Class[]{consumerType}, (p, m, args) -> {
            AxialConfigManager.get().hudEnabled = (Boolean) args[0];
            AxialConfigManager.save();
            updateEnabledTileLabel((Boolean) args[0]);
            refreshLayout();
            return null;
        });
        Method add = s.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, supplierType, consumerType);
        add.setAccessible(true);
        add.invoke(this, panelX + 18, 30, AxialConfigManager.get().hudEnabled ? "ENABLED" : "DISABLED", getter, setter);
        Field width = tiles.getLast().getClass().getDeclaredField("width");
        width.setAccessible(true);
        width.setInt(tiles.getLast(), 416);
    }

    @Unique private void addColor(int panelX, int y, String label, IntSupplier get, IntConsumer set) throws ReflectiveOperationException {
        Class<?> s = getClass(); String p = s.getName() + "$";
        Class<?> getterType = Class.forName(p + "ColorGetter"), setterType = Class.forName(p + "ColorSetter");
        Object getter = Proxy.newProxyInstance(getterType.getClassLoader(), new Class[]{getterType}, (q,m,args) -> get.getAsInt());
        Object setter = Proxy.newProxyInstance(setterType.getClassLoader(), new Class[]{setterType}, (q,m,args) -> { set.accept((Integer) args[0]); return null; });
        Method add = s.getDeclaredMethod("addColorOptionRow", int.class, int.class, String.class, getterType, setterType); add.setAccessible(true);
        add.invoke(this, panelX + 28, y, label, getter, setter);
        Field width = list("optionRows").getLast().getClass().getDeclaredField("width");
        width.setAccessible(true);
        width.setInt(list("optionRows").getLast(), 370);
    }
    @Unique private void updateEnabledTileLabel(boolean enabled) {
        try { for (Object tile : list("tiles")) { String label = text(tile, "label"); if ("ENABLED".equals(label) || "DISABLED".equals(label)) { Field field = tile.getClass().getDeclaredField("label"); field.setAccessible(true); field.set(tile, enabled ? "ENABLED" : "DISABLED"); return; } } }
        catch (ReflectiveOperationException ignored) { }
    }
    @Unique private void refreshLayout() { try { Method method = getClass().getDeclaredMethod("rebuildLayout"); method.setAccessible(true); method.invoke(this); } catch (ReflectiveOperationException ignored) { } }

    @Unique private void divider(DrawContext c, int x, int y, String label) { Text t = Text.literal(label).styled(s -> s.withFont(FONT)); var renderer = MinecraftClient.getInstance().textRenderer; c.drawTextWithShadow(renderer, t, x + 2, y, 0xFFC6D0F3); c.fill(x + renderer.getWidth(t) + 12, y + 5, x + getInt("panelWidth") - 28, y + 6, 0x998F5DFF); }
    @Unique private void drawScrollBar(DrawContext context, int panelX, int panelY) {
        int maximum = maxScroll();
        if (maximum == 0) return;
        int viewportTop = panelY + 30;
        int viewportHeight = getInt("panelHeight") - 48;
        int contentHeight = Math.max(viewportHeight + 1, CONTENT_HEIGHT - 48);
        int thumbHeight = Math.max(18, Math.round(viewportHeight * (viewportHeight / (float) contentHeight)));
        int thumbTravel = Math.max(1, viewportHeight - thumbHeight);
        int thumbY = viewportTop + Math.round(axial_cosmetics$scroll / (float) maximum * thumbTravel);
        int trackX = panelX + getInt("panelWidth") - 12;
        context.fill(trackX, viewportTop, trackX + 4, viewportTop + viewportHeight, 0x2AFFFFFF);
        context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFFB06AF3);
    }
    @Unique private void renderOptionRows(DrawContext context, int mouseX, int mouseY, int panelY) {
        try {
            for (Object row : list("optionRows")) {
                Method render = row.getClass().getDeclaredMethod("render", DrawContext.class, int.class, int.class, net.minecraft.client.font.TextRenderer.class, int.class);
                render.setAccessible(true);
                int rowMouseX = insideVisibleControl(row, mouseX, mouseY, panelY) ? mouseX : Integer.MIN_VALUE;
                render.invoke(row, context, rowMouseX, mouseY, MinecraftClient.getInstance().textRenderer, panelY);
                Object getter = value(row, "colorGetter");
                if (getter != null) {
                    Method getColor = getter.getClass().getDeclaredMethod("get");
                    getColor.setAccessible(true);
                    int color = (Integer) getColor.invoke(getter);
                    int x = integer(row, "x"), y = integer(row, "y") + panelY + 6;
                    int width = integer(row, "width");
                    Text hex = Text.literal(String.format("#%06X", color & 0xFFFFFF)).styled(style -> style.withFont(FONT));
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, hex,
                            x + width - 30 - MinecraftClient.getInstance().textRenderer.getWidth(hex), y, 0xFFC6D0F3);
                }
            }
        } catch (ReflectiveOperationException ignored) { }
    }
    @Unique private boolean insideVisibleControl(Object row, double mouseX, double mouseY, int panelY) throws ReflectiveOperationException {
        int x = integer(row, "x"), y = integer(row, "y") + panelY;
        if (mouseY < y - 2 || mouseY > y + 22) return false;
        if (value(row, "colorGetter") != null) {
            int width = integer(row, "width");
            return mouseX >= x + width - 24 && mouseX <= x + width + 4;
        }
        return mouseX >= x - 4 && mouseX <= x + 20;
    }
    @Unique private void shift(int delta) throws ReflectiveOperationException { for (Object r : list("optionRows")) shiftY(r, delta); for (Object t : list("tiles")) shiftY(t, delta); }
    @Unique private void removeBox() { try { list("tiles").removeIf(t -> "BOX".equals(text(t, "label"))); } catch (ReflectiveOperationException ignored) { } }
    @Unique private boolean isHud() {
        try {
            return !PotionsHudSettingsScreen.isPotions((Screen) (Object) this)
                    && "HUD".equals(String.valueOf(field("mode").get(this)));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
    @Unique private int maxScroll() { return Math.max(0, CONTENT_HEIGHT - getInt("panelHeight")); }
    @Unique private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    @SuppressWarnings("unchecked") @Unique private List<Object> list(String n) throws ReflectiveOperationException { return (List<Object>) field(n).get(this); }
    @Unique private Field field(String n) throws NoSuchFieldException { Field f = getClass().getDeclaredField(n); f.setAccessible(true); return f; }
    @Unique private int getInt(String n) { try { return field(n).getInt(this); } catch (ReflectiveOperationException e) { return 0; } }
    @Unique private void setInt(String n, int v) throws ReflectiveOperationException { field(n).setInt(this, v); }
    @Unique private static void shiftY(Object o, int d) throws ReflectiveOperationException { Field f = o.getClass().getDeclaredField("y"); f.setAccessible(true); f.setInt(o, f.getInt(o) + d); }
    @Unique private static String text(Object o, String n) { try { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return (String)f.get(o); } catch (ReflectiveOperationException e) { return ""; } }
    @Unique private static Object value(Object o, String n) throws ReflectiveOperationException { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f.get(o); }
    @Unique private static int integer(Object o, String n) throws ReflectiveOperationException { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f.getInt(o); }
}
