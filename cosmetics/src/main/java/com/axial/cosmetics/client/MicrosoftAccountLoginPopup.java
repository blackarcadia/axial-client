package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class MicrosoftAccountLoginPopup {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean_large"));
    private static final Path BASE_DIR = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher");
    private static final Path ACCOUNTS_DIR = BASE_DIR.resolve("accounts");
    private static final Path ACTIVE_ACCOUNT_POINTER = BASE_DIR.resolve("active-account.path");
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 210;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 10;

    private static boolean open;
    private static boolean loginStarted;
    private static boolean browserOpened;
    private static boolean success;
    private static String statusLine = "Starting Microsoft sign-in...";
    private static String verificationUri = "";
    private static String userCode = "";

    private MicrosoftAccountLoginPopup() {
    }

    public static void open() {
        open = true;
        loginStarted = false;
        browserOpened = false;
        success = false;
        statusLine = "Click Sign In to start authentication.";
        verificationUri = "";
        userCode = "";
    }

    public static boolean isOpen() {
        return open;
    }

    public static void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!open) {
            return;
        }

        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = Math.max(30, (screenHeight - PANEL_HEIGHT) / 2);

        context.fill(0, 0, screenWidth, screenHeight, 0x88000000);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE8181B22);
        context.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 2, 0x40FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xD08F5DFF);

        context.drawCenteredTextWithShadow(textRenderer, uiText("MICROSOFT SIGN-IN"), panelX + PANEL_WIDTH / 2, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("SIGN IN WITH YOUR MICROSOFT ACCOUNT."), panelX + PANEL_WIDTH / 2, panelY + 24, 0xFFC6D0F3);

        context.drawCenteredTextWithShadow(textRenderer, uiText(statusLine), panelX + PANEL_WIDTH / 2, panelY + 58, 0xFFFFFFFF);
        if (loginStarted) {
            if (!verificationUri.isBlank()) {
                context.drawCenteredTextWithShadow(textRenderer, uiText("VISIT"), panelX + PANEL_WIDTH / 2, panelY + 82, 0xFFC6D0F3);
                context.drawCenteredTextWithShadow(textRenderer, uiText(verificationUri), panelX + PANEL_WIDTH / 2, panelY + 96, 0xFFFFFFFF);
            }
            if (!userCode.isBlank()) {
                context.drawCenteredTextWithShadow(textRenderer, uiText("CODE: " + userCode), panelX + PANEL_WIDTH / 2, panelY + 122, 0xFFFFFFFF);
            }
            if (success) {
                context.drawCenteredTextWithShadow(textRenderer, uiText("ACCOUNT SAVED."), panelX + PANEL_WIDTH / 2, panelY + 148, 0xFF8AF0C2);
            }
        }

        drawButton(context, textRenderer, mouseX, mouseY, panelX + 72, panelY + 176, BUTTON_WIDTH, BUTTON_HEIGHT, "SIGN IN");
        drawButton(context, textRenderer, mouseX, mouseY, panelX + 72 + BUTTON_WIDTH + BUTTON_GAP, panelY + 176, BUTTON_WIDTH, BUTTON_HEIGHT, "OPEN BROWSER");
        drawButton(context, textRenderer, mouseX, mouseY, panelX + 72 + (BUTTON_WIDTH + BUTTON_GAP) * 2, panelY + 176, 80, BUTTON_HEIGHT, "CANCEL");
    }

    public static boolean mouseClicked(Click click, int screenWidth, int screenHeight) {
        if (!open) {
            return false;
        }

        int mouseX = (int) Math.round(click.x());
        int mouseY = (int) Math.round(click.y());
        int panelX = (screenWidth - PANEL_WIDTH) / 2;
        int panelY = Math.max(30, (screenHeight - PANEL_HEIGHT) / 2);

        if (!inRect(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
            return true;
        }

        int buttonY = panelY + 176;
        if (inRect(mouseX, mouseY, panelX + 72, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            startLogin();
            return true;
        }
        if (inRect(mouseX, mouseY, panelX + 72 + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            openBrowser();
            return true;
        }
        if (inRect(mouseX, mouseY, panelX + 72 + (BUTTON_WIDTH + BUTTON_GAP) * 2, buttonY, 80, BUTTON_HEIGHT)) {
            open = false;
            return true;
        }
        return true;
    }

    private static void startLogin() {
        if (loginStarted) {
            return;
        }
        loginStarted = true;
        statusLine = "Starting Microsoft sign-in...";
        CompletableFuture.runAsync(() -> {
            try {
                ParamMsaAuthServiceSupplier<java.util.function.Consumer<MsaDeviceCode>> supplier =
                        (client, appConfig, consumer) -> new DeviceCodeMsaAuthService(client, appConfig, code -> {
                            verificationUri = code.getVerificationUri();
                            userCode = code.getUserCode();
                            statusLine = "Use the code shown below to sign in.";
                            if (!browserOpened) {
                                browserOpened = true;
                                openBrowser(code.getDirectVerificationUri());
                            }
                            consumer.accept(code);
                        });

                JavaAuthManager authManager = JavaAuthManager.create(MinecraftAuth.createHttpClient("AxialLauncher/1.0"))
                        .login(supplier, code -> {});

                persist(authManager);
                statusLine = "Signed in as " + authManager.getMinecraftProfile().getUpToDate().getName() + ".";
                success = true;
                open = false;
                MinecraftClient.getInstance().execute(() -> {
                    refreshTitleDropdown();
                });
            } catch (Exception ex) {
                statusLine = "Login failed: " + ex.getMessage();
            }
        });
    }

    private static void persist(JavaAuthManager authManager) throws IOException {
        Files.createDirectories(ACCOUNTS_DIR);
        JsonObject json = JavaAuthManager.toJson(authManager);
        String name = authManager.getMinecraftProfile().getUpToDate().getName();
        Path target = ACCOUNTS_DIR.resolve(name + ".json");
        Files.writeString(target, json.toString(), StandardCharsets.UTF_8);
        writeActivePointer(target.getFileName().toString());
    }

    private static void openBrowser() {
        if (!verificationUri.isBlank()) {
            openBrowser(verificationUri);
        }
    }

    private static void openBrowser(String uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(java.net.URI.create(uri));
            }
        } catch (Exception ignored) {
        }
    }

    private static void refreshTitleDropdown() {
        TitleScreenAccountsDropdown.refresh();
    }

    private static void writeActivePointer(String fileName) {
        try {
            Files.createDirectories(ACTIVE_ACCOUNT_POINTER.getParent());
            Files.writeString(ACTIVE_ACCOUNT_POINTER, fileName, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static void drawButton(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int x, int y, int width, int height, String label) {
        boolean hovered = inRect(mouseX, mouseY, x, y, width, height);
        context.fill(x, y, x + width, y + height, hovered ? 0xBC20283A : 0xA0181D2C);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x33FFFFFF : 0x17FFFFFF);
        context.drawStrokedRectangle(x, y, width, height, hovered ? 0xFFE7D9FF : 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, uiText(label), x + width / 2, y + 6, 0xFFF7F7FF);
    }

    private static boolean inRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < (x + width) && mouseY >= y && mouseY < (y + height);
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }
}
