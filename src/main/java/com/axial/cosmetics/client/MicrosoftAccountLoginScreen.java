package com.axial.cosmetics.client;

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

import java.awt.Desktop;
import java.util.concurrent.CompletableFuture;

public final class MicrosoftAccountLoginScreen extends Screen {
    private static final StyleSpriteSource.Font UI_FONT = new StyleSpriteSource.Font(Identifier.of("axialutils", "ui_clean_large"));

    private final Screen parent;
    private boolean started;
    private volatile boolean cancelled;
    private volatile String statusLine = "Starting Microsoft sign-in...";
    private volatile String verificationUri = "";
    private volatile String userCode = "";
    private volatile boolean success;
    private volatile boolean browserOpened;
    private int panelX;
    private int panelY;
    private ButtonWidget openBrowserButton;
    private ButtonWidget cancelButton;

    public MicrosoftAccountLoginScreen(Screen parent) {
        super(uiText("MICROSOFT SIGN-IN"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildLayout();
        openBrowserButton = ButtonWidget.builder(uiText("OPEN BROWSER"), btn -> openBrowser()).build();
        cancelButton = ButtonWidget.builder(uiText("CANCEL"), btn -> close()).build();
        addDrawableChild(openBrowserButton);
        addDrawableChild(cancelButton);
        layoutButtons();
        startLogin();
    }

    @Override
    public void close() {
        cancelled = true;
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        rebuildLayout();
        MenuBackgroundRenderer.draw(context, this);
        drawPanel(context);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + 200, panelY + 10, 0xFFF7F7FF);
        context.drawCenteredTextWithShadow(textRenderer, uiText("SIGN IN WITH YOUR MICROSOFT ACCOUNT."), panelX + 200, panelY + 24, 0xFFC6D0F3);

        context.drawCenteredTextWithShadow(textRenderer, uiText(statusLine), panelX + 200, panelY + 58, 0xFFFFFFFF);
        if (!verificationUri.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, uiText("VISIT"), panelX + 200, panelY + 82, 0xFFC6D0F3);
            context.drawCenteredTextWithShadow(textRenderer, uiText(verificationUri), panelX + 200, panelY + 96, 0xFFFFFFFF);
        }
        if (!userCode.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, uiText("CODE: " + userCode), panelX + 200, panelY + 122, 0xFFFFFFFF);
        }
        if (success) {
            context.drawCenteredTextWithShadow(textRenderer, uiText("ACCOUNT SAVED."), panelX + 200, panelY + 148, 0xFF8AF0C2);
        }

        layoutButtons();
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void startLogin() {
        if (started) return;
        started = true;
        CompletableFuture.runAsync(() -> {
            try {
                ParamMsaAuthServiceSupplier<java.util.function.Consumer<MsaDeviceCode>> supplier =
                        (client, appConfig, consumer) -> new DeviceCodeMsaAuthService(client, appConfig, code -> {
                            if (cancelled) throw new java.util.concurrent.CancellationException();
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

                var prepared = AccountSessions.prepare(authManager);
                MinecraftClient.getInstance().execute(() -> {
                    if (cancelled || MinecraftClient.getInstance().currentScreen != this) return;
                    try {
                        String file = AccountStore.shared().save(authManager);
                        prepared.apply();
                        AccountStore.shared().select(file);
                        success = true;
                        close();
                    } catch (Exception ex) {
                        statusLine = "Could not finish sign-in. Please try again.";
                        cancelButton.setMessage(uiText("BACK"));
                    }
                });
            } catch (Exception ex) {
                statusLine = "Sign-in failed or expired. Go back and try again.";
            }
        });
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
        int buttonWidth = 116;
        int buttonY = panelY + 176;
        if (openBrowserButton != null) {
            openBrowserButton.setPosition(panelX + 72, buttonY);
            openBrowserButton.setWidth(buttonWidth);
            openBrowserButton.setHeight(20);
        }
        if (cancelButton != null) {
            cancelButton.setPosition(panelX + 212, buttonY);
            cancelButton.setWidth(buttonWidth);
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

    private static Text uiText(String value) {
        return Text.literal(value).styled(style -> style.withFont(UI_FONT));
    }

}
