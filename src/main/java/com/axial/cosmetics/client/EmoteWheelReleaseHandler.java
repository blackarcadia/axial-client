package com.axial.cosmetics.client;

import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.arch.EmotecraftClientMod;
import io.github.kosmx.emotes.arch.screen.ingame.FastMenuScreen;
import io.github.kosmx.emotes.arch.screen.widget.AbstractFastChooseWidget;
import io.github.kosmx.emotes.arch.screen.widget.IChooseElement;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Adds hold-and-release selection without intercepting Emotecraft's screen input methods.
 */
public final class EmoteWheelReleaseHandler {
    private static final int SEGMENT_COUNT = 8;
    private static Field fastMenuField;
    private static boolean wheelWasHeld;
    private static IChooseElement hoveredElement;
    private static FastMenuScreen wheelScreen;

    private EmoteWheelReleaseHandler() {
    }

    public static void tick(MinecraftClient client) {
        if (!Boolean.TRUE.equals(PlatformTools.getConfig().oldChooseWheel.get())) {
            reset();
            return;
        }

        boolean wheelHeld = EmotecraftClientMod.OPEN_MENU_KEY.isPressed();
        if (client.currentScreen instanceof FastMenuScreen screen && wheelHeld) {
            wheelWasHeld = true;
            wheelScreen = screen;
            hoveredElement = hoveredElement(client, screen);
            return;
        }

        if (wheelWasHeld && !wheelHeld && hoveredElement != null && hoveredElement.hasEmote()) {
            // Use Emotecraft's own selection path so its normal availability checks and
            // screen cleanup remain intact after the key-release event closes the wheel.
            wheelScreen.doHoverPart(hoveredElement);
        }
        reset();
    }

    private static IChooseElement hoveredElement(MinecraftClient client, FastMenuScreen screen) {
        AbstractFastChooseWidget wheel = wheel(screen);
        if (wheel == null) return null;

        int mouseX = (int) Math.round(client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
        int mouseY = (int) Math.round(client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());
        int centerX = wheel.getX() + wheel.getWidth() / 2;
        int centerY = wheel.getY() + wheel.getHeight() / 2;
        int size = Math.min(wheel.getWidth(), wheel.getHeight());
        int innerRadius = Math.max(46, Math.round(size * 0.19f));
        int outerRadius = Math.round(size * 0.49f);
        int segment = segmentAt(centerX, centerY, innerRadius, outerRadius, mouseX, mouseY);
        if (segment < 0) return null;

        // Emotecraft lays its legacy slots clockwise from the bottom, while the themed
        // wheel numbers segments clockwise from the top.
        int elementIndex = Math.floorMod(4 - segment, SEGMENT_COUNT);
        List<IChooseElement> elements = wheel.getChooseElements();
        return elementIndex < elements.size() ? elements.get(elementIndex) : null;
    }

    private static AbstractFastChooseWidget wheel(FastMenuScreen screen) {
        try {
            if (fastMenuField == null) {
                fastMenuField = FastMenuScreen.class.getDeclaredField("fastMenu");
                fastMenuField.setAccessible(true);
            }
            return (AbstractFastChooseWidget) fastMenuField.get(screen);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static int segmentAt(int centerX, int centerY, int innerRadius, int outerRadius, int mouseX, int mouseY) {
        double x = mouseX - centerX;
        double y = mouseY - centerY;
        double distance = Math.hypot(x, y);
        if (distance < innerRadius || distance > outerRadius) return -1;
        double segmentSize = Math.PI * 2 / SEGMENT_COUNT;
        return Math.floorMod((int) Math.floor((Math.PI / 2 - Math.atan2(y, x) + segmentSize / 2) / segmentSize), SEGMENT_COUNT);
    }

    private static void reset() {
        wheelWasHeld = false;
        hoveredElement = null;
        wheelScreen = null;
    }
}
