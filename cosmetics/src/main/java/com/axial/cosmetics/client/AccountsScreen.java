package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AccountsScreen extends Screen {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(net.minecraft.util.Identifier.of("axialutils", "ui_clean_large"));
    private static final Path ACCOUNTS_DIR = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher", "accounts");
    private static final Path ACTIVE_ACCOUNT_POINTER = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher", "active-account.path");

    private final Screen parent;
    private final List<AccountEntry> entries = new ArrayList<>();
    private String selectedFileName;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public AccountsScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        reloadEntries();
        rebuildLayout();
        rebuildButtons();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        rebuildLayout();
        MenuBackgroundRenderer.draw(context, this);
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, uiText("ACCOUNTS"), panelX + panelWidth / 2, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("SELECT THE ACCOUNT TO USE NEXT LAUNCH."), panelX + panelWidth / 2, panelY + 22, 0xFFC6D0F3);

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, uiText("NO SAVED ACCOUNTS."), panelX + panelWidth / 2, panelY + 72, 0xFFD8D8E2);
        } else {
            int y = panelY + 44;
            for (AccountEntry entry : entries) {
                boolean active = entry.fileName.equals(selectedFileName);
                drawAccountRow(context, entry, active, y);
                y += 26;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void rebuildLayout() {
        panelWidth = 360;
        panelHeight = Math.max(176, 90 + entries.size() * 26);
        panelX = Math.max(24, (width - panelWidth) / 2);
        panelY = Math.max(24, (height - panelHeight) / 2);
    }

    private void rebuildButtons() {
        clearChildren();

        int rowY = panelY + 44;
        for (AccountEntry entry : entries) {
            boolean active = entry.fileName.equals(selectedFileName);
            ButtonWidget row = ButtonWidget.builder(uiText(entry.displayName + (active ? "  [active]" : "")), btn -> selectAccount(entry))
                    .build();
            row.setPosition(panelX + 18, rowY);
            row.setWidth(panelWidth - 36);
            row.setHeight(22);
            addDrawableChild(row);
            rowY += 26;
        }

        ButtonWidget add = ButtonWidget.builder(uiText("ADD ACCOUNT"), btn -> openLauncherAccountManager()).build();
        add.setPosition(panelX + 18, panelY + panelHeight - 30);
        add.setWidth(110);
        add.setHeight(20);
        addDrawableChild(add);

        ButtonWidget logout = ButtonWidget.builder(uiText("LOG OUT"), btn -> logoutSelected()).build();
        logout.setPosition(panelX + 134, panelY + panelHeight - 30);
        logout.setWidth(100);
        logout.setHeight(20);
        addDrawableChild(logout);

        ButtonWidget close = ButtonWidget.builder(uiText("CLOSE"), btn -> close()).build();
        close.setPosition(panelX + panelWidth - 118, panelY + panelHeight - 30);
        close.setWidth(100);
        close.setHeight(20);
        addDrawableChild(close);
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE8101018);
        context.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 2, 0x44FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, panelWidth, panelHeight, 0xD08F5DFF);
    }

    private void drawAccountRow(DrawContext context, AccountEntry entry, boolean active, int y) {
        int x = panelX + 18;
        int rowWidth = panelWidth - 36;
        boolean hovered = false;
        context.fill(x, y, x + rowWidth, y + 22, active ? 0xBC20283A : 0xA0181D2C);
        context.drawStrokedRectangle(x, y, rowWidth, 22, active ? 0xFFE7D9FF : 0xD08F5DFF);
        context.drawTextWithShadow(textRenderer, uiText(entry.displayName), x + 10, y + 7, 0xFFFFFFFF);
        if (hovered) {
            context.fill(x, y, x + rowWidth, y + 22, 0x12000000);
        }
    }

    private void selectAccount(AccountEntry entry) {
        selectedFileName = entry.fileName;
        writeActivePointer(entry.fileName);
        rebuildButtons();
    }

    private void logoutSelected() {
        if (selectedFileName == null) {
            return;
        }

        try {
            Files.deleteIfExists(ACCOUNTS_DIR.resolve(selectedFileName));
            if (selectedFileName.equals(readActivePointer())) {
                Files.deleteIfExists(ACTIVE_ACCOUNT_POINTER);
            }
            reloadEntries();
            if (!entries.isEmpty()) {
                selectedFileName = entries.get(0).fileName;
                writeActivePointer(selectedFileName);
            } else {
                selectedFileName = null;
            }
            rebuildButtons();
        } catch (IOException ignored) {
        }
    }

    private void openLauncherAccountManager() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new MicrosoftAccountLoginScreen(client.currentScreen));
    }

    private void reloadEntries() {
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

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private record AccountEntry(String displayName, String uuid, String fileName) {
    }
}
