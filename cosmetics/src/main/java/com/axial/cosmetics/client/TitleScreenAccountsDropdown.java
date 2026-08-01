package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TitleScreenAccountsDropdown {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean_large"));
    private static final Path BASE_DIR = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher");
    private static final Path ACCOUNTS_DIR = BASE_DIR.resolve("accounts");
    private static final Path ACTIVE_ACCOUNT_POINTER = BASE_DIR.resolve("active-account.path");
    private static final Path ACTIVE_LAUNCHER_POINTER = BASE_DIR.resolve("active-launcher.path");
    private static final int ANCHOR_X = 20;
    private static final int ANCHOR_Y = 236;
    private static final int ANCHOR_WIDTH = 208;
    private static final int ANCHOR_HEIGHT = 22;
    private static final int PANEL_GAP = 6;
    private static final int PANEL_PADDING = 8;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int FOOTER_HEIGHT = 20;
    private static final int FOOTER_GAP = 6;
    private static final int MIN_PANEL_HEIGHT = 72;

    private static final List<AccountEntry> entries = new ArrayList<>();
    private static boolean open;
    private static String selectedFileName;
    private static long lastRefreshAt;

    private TitleScreenAccountsDropdown() {
    }

    public static void toggle() {
        open = !open;
        if (open) {
            refresh();
        }
    }

    public static boolean isOpen() {
        return open;
    }

    public static boolean clickToggleButton(Click click) {
        int mouseX = (int) Math.round(click.x());
        int mouseY = (int) Math.round(click.y());
        if (!inRect(mouseX, mouseY, ANCHOR_X, ANCHOR_Y, ANCHOR_WIDTH, ANCHOR_HEIGHT)) {
            return false;
        }

        toggle();
        return true;
    }

    public static void render(DrawContext context, TitleScreen screen, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (!open) {
            return;
        }

        refreshIfNeeded();

        int panelX = ANCHOR_X;
        int panelY = ANCHOR_Y + ANCHOR_HEIGHT + PANEL_GAP;
        int panelWidth = ANCHOR_WIDTH;
        int contentHeight = PANEL_PADDING * 2 + 12;
        if (entries.isEmpty()) {
            contentHeight += ROW_HEIGHT + 4;
        } else {
            contentHeight += entries.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        }
        contentHeight += FOOTER_GAP + FOOTER_HEIGHT;
        int panelHeight = Math.max(MIN_PANEL_HEIGHT, contentHeight);

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE8181B22);
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 2, 0x40FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, panelWidth, panelHeight, 0xD08F5DFF);

        int titleY = panelY + PANEL_PADDING;
        drawOutlinedText(context, textRenderer, uiText("ACCOUNTS"), panelX + PANEL_PADDING, titleY, 0xFFFFFFFF, 0xFF000000);

        int y = titleY + 14;
        if (entries.isEmpty()) {
            context.drawTextWithShadow(textRenderer, uiText("NO SAVED ACCOUNTS"), panelX + PANEL_PADDING, y + 4, 0xFFE0E0E8);
        } else {
            for (AccountEntry entry : entries) {
                boolean active = entry.fileName.equals(selectedFileName);
                int rowX = panelX + PANEL_PADDING;
                int rowWidth = panelWidth - (PANEL_PADDING * 2);
                boolean hovered = inRect(mouseX, mouseY, rowX, y, rowWidth, ROW_HEIGHT);
                int fill = active ? 0xBC20283A : 0xAA131826;
                if (hovered) {
                    fill = active ? 0xCE2A3550 : 0xBF1F2638;
                }
                context.fill(rowX, y, rowX + rowWidth, y + ROW_HEIGHT, fill);
                context.drawStrokedRectangle(rowX, y, rowWidth, ROW_HEIGHT, active ? 0xFFE7D9FF : 0xD08F5DFF);
                String label = entry.displayName + (active ? "  [active]" : "");
                drawOutlinedText(context, textRenderer, uiText(label), rowX + 8, y + 6, 0xFFFFFFFF, 0xFF000000);
                y += ROW_HEIGHT + ROW_GAP;
            }
        }

        int footerY = panelY + panelHeight - PANEL_PADDING - FOOTER_HEIGHT;
        int footerWidth = (panelWidth - (PANEL_PADDING * 3)) / 2;
        int footerX = panelX + PANEL_PADDING;
        drawFooterButton(context, textRenderer, mouseX, mouseY, footerX, footerY, footerWidth, FOOTER_HEIGHT, "ADD ACCOUNT");
        drawFooterButton(context, textRenderer, mouseX, mouseY, footerX + footerWidth + PANEL_PADDING, footerY, footerWidth, FOOTER_HEIGHT, "LOG OUT");
    }

    public static boolean mouseClicked(Click click) {
        if (!open) {
            return false;
        }

        int mouseX = (int) Math.round(click.x());
        int mouseY = (int) Math.round(click.y());
        int panelX = ANCHOR_X;
        int panelY = ANCHOR_Y + ANCHOR_HEIGHT + PANEL_GAP;
        int panelWidth = ANCHOR_WIDTH;
        int contentHeight = PANEL_PADDING * 2 + 12;
        if (entries.isEmpty()) {
            contentHeight += ROW_HEIGHT + 4;
        } else {
            contentHeight += entries.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        }
        contentHeight += FOOTER_GAP + FOOTER_HEIGHT;
        int panelHeight = Math.max(MIN_PANEL_HEIGHT, contentHeight);
        if (!inRect(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight)) {
            open = false;
            return false;
        }

        int y = panelY + PANEL_PADDING + 14;
        if (!entries.isEmpty()) {
            for (AccountEntry entry : entries) {
                int rowX = panelX + PANEL_PADDING;
                int rowWidth = panelWidth - (PANEL_PADDING * 2);
                if (inRect(mouseX, mouseY, rowX, y, rowWidth, ROW_HEIGHT)) {
                    selectAccount(entry);
                    return true;
                }
                y += ROW_HEIGHT + ROW_GAP;
            }
        }

        int footerY = panelY + panelHeight - PANEL_PADDING - FOOTER_HEIGHT;
        int footerWidth = (panelWidth - (PANEL_PADDING * 3)) / 2;
        int footerX = panelX + PANEL_PADDING;
        if (inRect(mouseX, mouseY, footerX, footerY, footerWidth, FOOTER_HEIGHT)) {
            openClientLogin();
            open = false;
            return true;
        }
        if (inRect(mouseX, mouseY, footerX + footerWidth + PANEL_PADDING, footerY, footerWidth, FOOTER_HEIGHT)) {
            logoutSelected();
            return true;
        }

        return true;
    }

    private static void refreshIfNeeded() {
        if (System.currentTimeMillis() - lastRefreshAt > 2000L) {
            refresh();
        }
    }

    private static void refresh() {
        entries.clear();
        selectedFileName = readActivePointer();
        if (Files.isDirectory(ACCOUNTS_DIR)) {
            try (var stream = Files.list(ACCOUNTS_DIR)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(path -> {
                            AccountEntry entry = parseAccount(path);
                            if (entry != null) {
                                entries.add(entry);
                            }
                        });
            } catch (IOException ignored) {
            }
        }
        if (selectedFileName == null && !entries.isEmpty()) {
            selectedFileName = entries.get(0).fileName;
        }
        lastRefreshAt = System.currentTimeMillis();
    }

    private static void selectAccount(AccountEntry entry) {
        selectedFileName = entry.fileName;
        writeActivePointer(entry.fileName);
    }

    private static void logoutSelected() {
        if (selectedFileName == null) {
            return;
        }

        try {
            Files.deleteIfExists(ACCOUNTS_DIR.resolve(selectedFileName));
            if (selectedFileName.equals(readActivePointer())) {
                Files.deleteIfExists(ACTIVE_ACCOUNT_POINTER);
            }
            refresh();
            if (!entries.isEmpty()) {
                selectedFileName = entries.get(0).fileName;
                writeActivePointer(selectedFileName);
            } else {
                selectedFileName = null;
            }
        } catch (IOException ignored) {
        }
    }

    private static void drawFooterButton(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int x, int y, int width, int height, String label) {
        boolean hovered = inRect(mouseX, mouseY, x, y, width, height);
        context.fill(x, y, x + width, y + height, hovered ? 0xBF20283A : 0xAA131826);
        context.drawStrokedRectangle(x, y, width, height, hovered ? 0xFFE7D9FF : 0xD08F5DFF);
        drawOutlinedText(context, textRenderer, uiText(label), x + 8, y + 6, 0xFFFFFFFF, 0xFF000000);
    }

    private static void openClientLogin() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            client.setScreen(new MicrosoftAccountLoginScreen(client.currentScreen));
        }
    }

    private static AccountEntry parseAccount(Path path) {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject profile = root.getAsJsonObject("minecraftProfile");
            if (profile == null) {
                return null;
            }
            String name = profile.has("name") ? profile.get("name").getAsString() : path.getFileName().toString();
            String uuid = profile.has("id") ? profile.get("id").getAsString() : "";
            return new AccountEntry(name, uuid, path.getFileName().toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeActivePointer(String fileName) {
        try {
            Files.createDirectories(ACTIVE_ACCOUNT_POINTER.getParent());
            Files.writeString(ACTIVE_ACCOUNT_POINTER, fileName);
        } catch (IOException ignored) {
        }
    }

    private static String readActivePointer() {
        try {
            if (Files.exists(ACTIVE_ACCOUNT_POINTER)) {
                String value = Files.readString(ACTIVE_ACCOUNT_POINTER).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static void drawOutlinedText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int fillColor, int outlineColor) {
        context.drawText(textRenderer, text, x - 1, y, outlineColor, false);
        context.drawText(textRenderer, text, x + 1, y, outlineColor, false);
        context.drawText(textRenderer, text, x, y - 1, outlineColor, false);
        context.drawText(textRenderer, text, x, y + 1, outlineColor, false);
        context.drawText(textRenderer, text, x, y, fillColor, false);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private static boolean inRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < (x + width) && mouseY >= y && mouseY < (y + height);
    }

    private record AccountEntry(String displayName, String uuid, String fileName) {
    }
}
