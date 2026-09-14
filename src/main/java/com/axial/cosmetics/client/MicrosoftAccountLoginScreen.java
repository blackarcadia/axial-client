package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import org.example.launcher.MicrosoftLoginProcess;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public final class MicrosoftAccountLoginScreen extends Screen {
    private final Screen parent;
    private MicrosoftLoginProcess login;
    private boolean started;
    private boolean cancelled;
    private boolean busy;
    private String status = "Opening Microsoft sign-in...";
    private ButtonWidget retryButton;

    public MicrosoftAccountLoginScreen(Screen parent) {
        super(Text.literal("MICROSOFT SIGN-IN"));
        this.parent = parent;
    }

    @Override protected void init() {
        int centerY = height / 2;
        retryButton = addDrawableChild(ButtonWidget.builder(Text.literal("TRY AGAIN"), button -> startLogin())
                .dimensions(width / 2 - 126, centerY + 40, 120, 20).build());
        retryButton.active = !busy;
        addDrawableChild(ButtonWidget.builder(Text.literal("BACK"), button -> close())
                .dimensions(width / 2 + 6, centerY + 40, 120, 20).build());
        if (!started) startLogin();
    }

    private void startLogin() {
        if (busy || cancelled) return;
        started = true;
        busy = true;
        retryButton.active = false;
        status = "Complete sign-in in the Axial Microsoft window.";
        login = new MicrosoftLoginProcess();
        MicrosoftLoginProcess attempt = login;
        CompletableFuture.supplyAsync(() -> {
            try {
                var auth = JavaAuthManager.fromJson(MinecraftAuth.createHttpClient("AxialLauncher/1.0"), attempt.authenticate());
                return new LoginResult(auth, AccountSessions.prepare(auth));
            } catch (Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        }).whenComplete((result, error) -> MinecraftClient.getInstance().execute(() -> {
            if (cancelled || login != attempt) return;
            busy = false;
            retryButton.active = true;
            if (error != null) {
                Throwable cause = error.getCause();
                status = cause instanceof CancellationException ? "Sign-in cancelled. You can try again."
                        : "Sign-in failed. Check your launcher is updated and retry.";
                return;
            }
            if (MinecraftClient.getInstance().currentScreen != this) return;
            try {
                String file = AccountStore.shared().save(result.auth());
                result.session().apply();
                try { AccountStore.shared().select(file); }
                catch (java.io.IOException ex) {
                    status = "Signed in, but could not save the startup account.";
                    return;
                }
                close();
            } catch (Exception ex) { status = "Could not finish sign-in. Please try again."; }
        }));
    }

    @Override public void close() {
        cancelled = true;
        if (login != null) login.close();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override public void removed() {
        cancelled = true;
        if (login != null) login.close();
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MenuBackgroundRenderer.draw(context, this);
        int panelWidth = Math.min(420, width - 20);
        int x = (width - panelWidth) / 2;
        int y = height / 2 - 72;
        context.fill(x, y, x + panelWidth, y + 148, 0xED101018);
        context.drawStrokedRectangle(x, y, panelWidth, 148, 0xD08F5DFF);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, y + 14, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Sign in securely with your Microsoft account."), width / 2, y + 38, 0xFFC6D0F3);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(status, panelWidth - 20)), width / 2, y + 62, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private record LoginResult(JavaAuthManager auth, AccountSessions.Prepared session) {}
}
