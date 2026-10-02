package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HudColorDefaults;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Satchel Helper submenu using the Information HUD's compact, scrollable layout. */
@Mixin(targets = "org.axial.axialutils.client.AxialConfigScreen", remap = false, priority = 1500)
public abstract class AxialConfigScreenSatchelMenuMixin extends Screen {
    @Unique private static final int WIDTH = 452, HEIGHT = 168, CONTENT_HEIGHT = 228;
    @Unique private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean"));
    @Unique private int axial_cosmetics$satchelScroll;
    @Unique private TextFieldWidget axial_cosmetics$satchelTitle;

    protected AxialConfigScreenSatchelMenuMixin(Text title) { super(title); }

    @Inject(method = "method_25426", at = @At("TAIL"), remap = false)
    private void initTitleField(CallbackInfo ci) {
        axial_cosmetics$satchelTitle = addSelectableChild(new TextFieldWidget(textRenderer, 0, 0, 370, 20, label("DISPLAY TITLE")));
        axial_cosmetics$satchelTitle.setChangedListener(value -> {
            AxialConfigManager.get().satchelHudTitleText = value.substring(0, Math.min(48, value.length()));
            AxialConfigManager.save();
        });
    }

    @Inject(method = "rebuildLayout", at = @At("HEAD"), remap = false, order = 3000)
    private void rememberScroll(CallbackInfo ci) { if (isSatchel()) axial_cosmetics$satchelScroll = getInt("submenuScrollOffset"); }

    @Inject(method = "rebuildLayout", at = @At("RETURN"), remap = false, order = 3000)
    private void buildSatchel(CallbackInfo ci) {
        if (!isSatchel()) return;
        try {
            List<Object> tiles = list("tiles"), rows = list("optionRows");
            tiles.clear(); rows.clear();
            int panelX = (width - WIDTH) / 2;
            addEnabledTile(tiles, panelX);
            addColor(panelX, 84, "TITLE COLOR", () -> AxialConfigManager.get().satchelHudTitleColor, v -> { AxialConfigManager.get().satchelHudTitleColor = v; AxialConfigManager.save(); });
            addColor(panelX, 114, "SATCHEL COUNT COLOR", () -> AxialConfigManager.get().satchelHudCountColor, v -> { AxialConfigManager.get().satchelHudCountColor = v; AxialConfigManager.save(); });
            addColor(panelX, 144, "SATCHEL EMPTY COLOR", () -> AxialConfigManager.get().satchelHudEmptyColor, v -> { AxialConfigManager.get().satchelHudEmptyColor = v; AxialConfigManager.save(); });
            setInt("panelWidth", WIDTH); setInt("panelHeight", Math.min(height - 32, HEIGHT)); setInt("panelHeightAnimated", getInt("panelHeight"));
            setInt("submenuContentHeight", CONTENT_HEIGHT); setInt("panelTargetY", (height - getInt("panelHeight")) / 2);
            axial_cosmetics$satchelScroll = clamp(axial_cosmetics$satchelScroll, 0, maxScroll()); setInt("submenuScrollOffset", axial_cosmetics$satchelScroll);
            shift(-axial_cosmetics$satchelScroll);
            if (axial_cosmetics$satchelTitle != null) axial_cosmetics$satchelTitle.setText(AxialConfigManager.get().satchelHudTitleText == null ? "SATCHEL HUD" : AxialConfigManager.get().satchelHudTitleText);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Could not build Satchel Helper settings", e); }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void scrollSatchel(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (!isSatchel() || vertical == 0 || maxScroll() == 0) return;
        int next = clamp(axial_cosmetics$satchelScroll + (vertical < 0 ? 18 : -18), 0, maxScroll());
        try { shift(axial_cosmetics$satchelScroll - next); axial_cosmetics$satchelScroll = next; setInt("submenuScrollOffset", next); cir.setReturnValue(true); }
        catch (ReflectiveOperationException ignored) { }
    }

    @Inject(method = "drawScrollBar", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressSharedScrollBar(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) { if (isSatchel()) ci.cancel(); }

    @Inject(method = "drawPanel", at = @At("TAIL"), remap = false, order = 3000)
    private void drawSatchel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isSatchel()) return;
        int panelX = (width - getInt("panelWidth")) / 2, panelY = (height - getInt("panelHeight")) / 2;
        context.enableScissor(panelX, panelY + 26, panelX + getInt("panelWidth"), panelY + getInt("panelHeight") - 14);
        try {
            renderRows(context, mouseX, mouseY, panelY);
            divider(context, panelX + 18, panelY + 58 - axial_cosmetics$satchelScroll, "COLOR");
            divider(context, panelX + 18, panelY + 174 - axial_cosmetics$satchelScroll, "DISPLAY TITLE");
            positionTitle(context, panelX, panelY, mouseX, mouseY, delta);
            drawScrollBar(context, panelX, panelY);
        } finally { context.disableScissor(); }
    }

    @Inject(method = "method_25402", at = @At("HEAD"), cancellable = true, remap = false)
    private void clickColor(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!isSatchel() || click.button() != 0) return;
        try {
            int panelY = (height - getInt("panelHeight")) / 2;
            for (Object row : list("optionRows")) {
                if (!insideSwatch(row, click.x(), click.y(), panelY)) continue;
                Method activate = row.getClass().getDeclaredMethod("activate"); activate.setAccessible(true); activate.invoke(row);
                cir.setReturnValue(true); return;
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    @Unique private void addEnabledTile(List<Object> tiles, int panelX) throws ReflectiveOperationException {
        Class<?> s = getClass(), supplier = Class.forName(s.getName() + "$BooleanSupplier"), consumer = Class.forName(s.getName() + "$BooleanConsumer");
        Object get = Proxy.newProxyInstance(supplier.getClassLoader(), new Class[]{supplier}, (p,m,a) -> Boolean.TRUE.equals(AxialConfigManager.get().showSatchelsHud));
        Object set = Proxy.newProxyInstance(consumer.getClassLoader(), new Class[]{consumer}, (p,m,a) -> { AxialConfigManager.get().showSatchelsHud = (Boolean) a[0]; AxialConfigManager.save(); return null; });
        Method add = s.getDeclaredMethod("addToggleTile", int.class, int.class, String.class, supplier, consumer); add.setAccessible(true); add.invoke(this, panelX + 18, 30, "ENABLED", get, set);
        setField(tiles.getLast(), "width", 416);
    }

    @Unique private void addColor(int panelX, int y, String label, IntSupplier get, IntConsumer set) throws ReflectiveOperationException {
        Class<?> s = getClass(); String p = s.getName() + "$";
        Class<?> getter = Class.forName(p + "ColorGetter"), setter = Class.forName(p + "ColorSetter");
        Object g = Proxy.newProxyInstance(getter.getClassLoader(), new Class[]{getter}, (q,m,a) -> get.getAsInt());
        Object t = Proxy.newProxyInstance(setter.getClassLoader(), new Class[]{setter}, (q,m,a) -> { set.accept((Integer) a[0]); return null; });
        Method add = s.getDeclaredMethod("addColorOptionRow", int.class, int.class, String.class, getter, setter); add.setAccessible(true); add.invoke(this, panelX + 28, y, label, g, t);
        setField(list("optionRows").getLast(), "width", 370);
    }

    @Unique private void renderRows(DrawContext c, int mouseX, int mouseY, int panelY) {
        try { for (Object row : list("optionRows")) { Method render = row.getClass().getDeclaredMethod("render", DrawContext.class, int.class, int.class, net.minecraft.client.font.TextRenderer.class, int.class); render.setAccessible(true); render.invoke(row, c, mouseX, mouseY, textRenderer, panelY); drawHex(c, row, panelY); } }
        catch (ReflectiveOperationException ignored) { }
    }
    @Unique private void drawHex(DrawContext c, Object row, int panelY) throws ReflectiveOperationException {
        Object getter = field(row, "colorGetter").get(row); if (getter == null) return;
        Method get = getter.getClass().getDeclaredMethod("get"); get.setAccessible(true); int color = (Integer) get.invoke(getter);
        int x = integer(row, "x"), y = integer(row, "y") + panelY + 6, rowWidth = integer(row, "width");
        Text hex = label(String.format("#%06X", color & 0xFFFFFF));
        c.drawTextWithShadow(textRenderer, hex, x + rowWidth - 30 - textRenderer.getWidth(hex), y, 0xFFC6D0F3);
    }
    @Unique private void positionTitle(DrawContext context, int panelX, int panelY, int mouseX, int mouseY, float delta) {
        if (axial_cosmetics$satchelTitle == null) return;
        int y = panelY + 198 - axial_cosmetics$satchelScroll;
        axial_cosmetics$satchelTitle.setPosition(panelX + 28, y); axial_cosmetics$satchelTitle.visible = y >= panelY + 26 && y + 20 <= panelY + getInt("panelHeight") - 14;
        if (axial_cosmetics$satchelTitle.visible) axial_cosmetics$satchelTitle.render(context, mouseX, mouseY, delta);
    }

    @Unique private void divider(DrawContext c, int x, int y, String text) { Text t = label(text); c.drawTextWithShadow(textRenderer, t, x + 2, y, 0xFFC6D0F3); c.fill(x + textRenderer.getWidth(t) + 12, y + 5, x + getInt("panelWidth") - 28, y + 6, 0x998F5DFF); }
    @Unique private void drawScrollBar(DrawContext c, int x, int y) { int max = maxScroll(); if (max == 0) return; int top = y + 30, h = getInt("panelHeight") - 48, content = Math.max(h + 1, CONTENT_HEIGHT - 48), thumb = Math.max(18, Math.round(h * (h / (float) content))), travel = Math.max(1, h - thumb), thumbY = top + Math.round(axial_cosmetics$satchelScroll / (float) max * travel), track = x + getInt("panelWidth") - 12; c.fill(track, top, track + 4, top + h, 0x2AFFFFFF); c.fill(track, thumbY, track + 4, thumbY + thumb, 0xFFB06AF3); }
    @Unique private boolean insideSwatch(Object row, double x, double y, int offset) throws ReflectiveOperationException { int rowX = integer(row, "x"), rowY = integer(row, "y") + offset, w = integer(row, "width"); return x >= rowX + w - 24 && x <= rowX + w + 4 && y >= rowY - 2 && y <= rowY + 22; }
    @Unique private void shift(int d) throws ReflectiveOperationException { for (Object r : list("optionRows")) setField(r, "y", integer(r, "y") + d); for (Object t : list("tiles")) setField(t, "y", integer(t, "y") + d); }
    @Unique private boolean isSatchel() { try { return "SATCHEL".equals(String.valueOf(field(this, "mode").get(this))); } catch (ReflectiveOperationException e) { return false; } }
    @Unique private int maxScroll() { return Math.max(0, CONTENT_HEIGHT - getInt("panelHeight")); }
    @Unique private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    @Unique private static Text label(String s) { return Text.literal(s).styled(style -> style.withFont(FONT)); }
    @SuppressWarnings("unchecked") @Unique private List<Object> list(String n) throws ReflectiveOperationException { return (List<Object>) field(this, n).get(this); }
    @Unique private Field field(Object o, String n) throws NoSuchFieldException { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f; }
    @Unique private int getInt(String n) { try { return field(this, n).getInt(this); } catch (ReflectiveOperationException e) { return 0; } }
    @Unique private void setInt(String n, int v) throws ReflectiveOperationException { field(this, n).setInt(this, v); }
    @Unique private static int integer(Object o, String n) throws ReflectiveOperationException { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f.getInt(o); }
    @Unique private static void setField(Object o, String n, int v) throws ReflectiveOperationException { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); f.setInt(o, v); }
}
