package org.example.launcher;

import javafx.application.Platform;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.service.impl.JfxWebViewMsaAuthService;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs the embedded web view in its own UI process, separate from Minecraft's GLFW thread. */
public final class EmbeddedMicrosoftLogin {
    private EmbeddedMicrosoftLogin() {}

    public static void main(String[] args) {
        int exitCode = 1;
        try {
            if (args.length != 1) throw new IllegalArgumentException("Missing result file");
            // Each process gets a fresh WebView/cookie session, allowing a different account every time.
            SwingUtilities.invokeAndWait(() -> new javafx.embed.swing.JFXPanel());
            Platform.setImplicitExit(false);
            var auth = JavaAuthManager.create(MinecraftAuth.createHttpClient()).login((http, config) ->
                    new JfxWebViewMsaAuthService(http, config,
                            window -> SwingUtilities.invokeLater(() -> {
                                window.setTitle("Axial • Sign in with Microsoft");
                                window.setResizable(true);
                                window.setVisible(true);
                                window.toFront();
                            }),
                            window -> SwingUtilities.invokeLater(window::dispose)));
            auth.getMinecraftToken().getUpToDate();
            auth.getMinecraftProfile().getUpToDate();
            Files.writeString(Path.of(args[0]), JavaAuthManager.toJson(auth).toString());
            exitCode = 0;
        } catch (JfxWebViewMsaAuthService.UserClosedWindowException ex) {
            exitCode = 2;
        } catch (Throwable ex) {
            // Do not print authentication responses, tokens or redirect URLs to launcher logs.
            System.err.println("Embedded Microsoft sign-in could not complete.");
        } finally {
            System.exit(exitCode);
        }
    }
}
