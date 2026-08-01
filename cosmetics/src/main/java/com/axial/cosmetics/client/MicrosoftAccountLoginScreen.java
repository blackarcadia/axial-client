package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier;

import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class MicrosoftAccountLoginScreen extends Screen {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean_large"));
    private static final Path BASE_DIR = Path.of(System.getProperty("user.home"), "Library", "Application Support", "AxialLauncher");
    private static final Path ACCOUNTS_DIR = BASE_DIR.resolve("accounts");
    private static final Path ACTIVE_ACCOUNT_POINTER = BASE_DIR.resolve("active-account.path");

    private final Screen parent;
    private final CompletableFuture<Void> authFuture = new CompletableFuture<>();
    private volatile String statusLine = "Starting Microsoft sign-in...";
    private volatile String verificationUri = "";
    private volatile String userCode = "";
    private volatile boolean success;
    private volatile boolean browserOpened;
    private volatile boolean loginStarted;
    private int panelX;
    private int panelY;
    private ButtonWidget signInButton;
    private ButtonWidget openBrowserButton;
    private ButtonWidget cancelButton;

    public MicrosoftAccountLoginScreen(Screen parent) {
        super(uiText("MICROSOFT SIGN-IN"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildLayout();
        signInButton = ButtonWidget.builder(uiText("SIGN IN"), btn -> startLogin()).build();
        openBrowserButton = ButtonWidget.builder(uiText("OPEN BROWSER"), btn -> openBrowser()).build();
        cancelButton = ButtonWidget.builder(uiText("CANCEL"), btn -> close()).build();
        addDrawableChild(signInButton);
        addDrawableChild(openBrowserButton);
        addDrawableChild(cancelButton);
        layoutButtons();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        rebuildLayout();
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + 200, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("SIGN IN WITH YOUR MICROSOFT ACCOUNT."), panelX + 200, panelY + 24, 0xFFC6D0F3);

        context.drawCenteredTextWithShadow(textRenderer, uiText(statusLine), panelX + 200, panelY + 58, 0xFFFFFFFF);
        if (loginStarted) {
            if (!verificationUri.isBlank()) {
                context.drawCenteredTextWithShadow(textRenderer, uiText("VISIT"), panelX + 200, panelY + 82, 0xFFC6D0F3);
                context.drawCenteredTextWithShadow(textRenderer, uiText(verificationUri), panelX + 200, panelY + 96, 0xFFFFFFFF);
            }
            if (!userCode.isBlank()) {
                context.drawCenteredTextWithShadow(textRenderer, uiText("CODE: " + userCode), panelX + 200, panelY + 122, 0xFFFFFFFF);
            }
            if (success) {
                context.drawCenteredTextWithShadow(textRenderer, uiText("ACCOUNT SAVED. RETURNING TO TITLE..."), panelX + 200, panelY + 148, 0xFF8AF0C2);
            }
        } else {
            context.drawCenteredTextWithShadow(textRenderer, uiText("CLICK SIGN IN TO START AUTHENTICATION."), panelX + 200, panelY + 82, 0xFFC6D0F3);
        }

        layoutButtons();
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void startLogin() {
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
                AuthData data = new AuthData(
                        authManager.getMinecraftProfile().getUpToDate().getName(),
                        authManager.getMinecraftProfile().getUpToDate().getId().toString(),
                        authManager.getMinecraftToken().getUpToDate().getToken(),
                        authManager.getJavaXstsToken().getUpToDate().getUserHash()
                );
                authFuture.complete(null);
                statusLine = "Signed in as " + data.playerName + ".";
                success = true;
                writeActivePointer(data.fileName());
                SwingUtilities.invokeLater(() -> {
                    if (MinecraftClient.getInstance().currentScreen == this) {
                        close();
                    }
                });
            } catch (Exception ex) {
                statusLine = "Login failed: " + ex.getMessage();
                authFuture.completeExceptionally(ex);
            }
        });
    }

    private void persist(JavaAuthManager authManager) throws IOException {
        Files.createDirectories(ACCOUNTS_DIR);
        JsonObject json = JavaAuthManager.toJson(authManager);
        String name = authManager.getMinecraftProfile().getUpToDate().getName();
        Path target = ACCOUNTS_DIR.resolve(name + ".json");
        Files.writeString(target, json.toString(), StandardCharsets.UTF_8);
        writeActivePointer(target.getFileName().toString());
    }

    private void openBrowser() {
        if (!verificationUri.isBlank()) {
            openBrowser(verificationUri);
        }
    }

    private void openBrowser(String uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(java.net.URI.create(uri));
            }
        } catch (Exception ignored) {
        }
    }

    private void layoutButtons() {
        int buttonWidth = 90;
        int buttonY = panelY + 176;
        if (signInButton != null) {
            signInButton.setPosition(panelX + 60, buttonY);
            signInButton.setWidth(buttonWidth);
            signInButton.setHeight(20);
        }
        if (openBrowserButton != null) {
            openBrowserButton.setPosition(panelX + 160, buttonY);
            openBrowserButton.setWidth(buttonWidth);
            openBrowserButton.setHeight(20);
        }
        if (cancelButton != null) {
            cancelButton.setPosition(panelX + 260, buttonY);
            cancelButton.setWidth(80);
            cancelButton.setHeight(20);
        }
    }

    private void rebuildLayout() {
        panelX = (width - 400) / 2;
        panelY = Math.max(30, (height - 200) / 2);
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX, panelY, panelX + 400, panelY + 200, 0xE8181B22);
        context.fill(panelX + 1, panelY + 1, panelX + 399, panelY + 2, 0x40FFFFFF);
        context.drawStrokedRectangle(panelX, panelY, 400, 200, 0xD08F5DFF);
    }

    private static void writeActivePointer(String fileName) {
        try {
            Files.createDirectories(ACTIVE_ACCOUNT_POINTER.getParent());
            Files.writeString(ACTIVE_ACCOUNT_POINTER, fileName, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

    private record AuthData(String playerName, String uuid, String accessToken, String xuid) {
        String fileName() {
            return playerName + ".json";
        }
    }
}
