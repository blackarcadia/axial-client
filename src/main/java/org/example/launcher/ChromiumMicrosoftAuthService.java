package org.example.launcher;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.*;

final class ChromiumMicrosoftAuthService extends MsaAuthService {
    private final MicrosoftBrowserRequest request;
    private final java.nio.file.Path browserDirectory;

    ChromiumMicrosoftAuthService(HttpClient http, MsaApplicationConfig config, java.nio.file.Path browserDirectory) {
        super(http, config);
        request = new MicrosoftBrowserRequest(config);
        this.browserDirectory = browserDirectory;
    }

    @Override public MsaToken acquireToken() throws IOException, InterruptedException, TimeoutException {
        var code = new CompletableFuture<String>();
        var terminated = new CompletableFuture<Void>();
        var frameHolder = new JFrame[1];
        var status = new JLabel("Preparing Microsoft sign-in...", SwingConstants.CENTER);
        CefApp app = null;
        try {
            SwingUtilities.invokeAndWait(() -> {
                var window = new JFrame("Axial • Sign in with Microsoft");
                frameHolder[0] = window;
                window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                window.setSize(840, 680);
                window.setMinimumSize(new java.awt.Dimension(480, 480));
                window.setLocationRelativeTo(null);
                window.add(status, BorderLayout.CENTER);
                window.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosing(WindowEvent event) {
                        code.completeExceptionally(new CancellationException("Sign-in cancelled"));
                        window.setVisible(false);
                    }
                });
                window.setVisible(true);
            });
            var builder = new CefAppBuilder();
            builder.setInstallDir(ClientPaths.appRoot().resolve("browser-runtime").resolve("chromium-146.0.7680.179")
                    .resolve(System.getProperty("os.arch")).toFile());
            builder.getCefSettings().windowless_rendering_enabled = false;
            builder.getCefSettings().cache_path = ""; // In-memory cookies; no shared account session.
            builder.getCefSettings().persist_session_cookies = false;
            builder.getCefSettings().root_cache_path = Files.createDirectories(browserDirectory).toAbsolutePath().toString();
            builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
            builder.setProgressHandler((stage, progress) -> SwingUtilities.invokeLater(() ->
                    status.setText("Preparing Microsoft sign-in...")));
            builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                @Override public void stateHasChanged(CefApp.CefAppState state) {
                    if (state == CefApp.CefAppState.TERMINATED) {
                        terminated.complete(null);
                        code.completeExceptionally(new IOException("Sign-in window closed"));
                    }
                }
            });
            app = builder.build();
            if (code.isDone()) return awaitToken(code);
            var client = app.createClient();
            client.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                    if (frame.isMain()) SwingUtilities.invokeLater(() -> status.setText(
                            httpStatusCode >= 400 ? "Microsoft's sign-in page is unavailable. Please try again later." : " "));
                }

                @Override public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode error,
                                                  String errorText, String failedUrl) {
                    if (frame.isMain() && error != ErrorCode.ERR_ABORTED) SwingUtilities.invokeLater(() ->
                            status.setText("Could not load Microsoft sign-in. Check your connection and try again."));
                }
            });
            client.addRequestHandler(new CefRequestHandlerAdapter() {
                @Override public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest navigation,
                                                        boolean gesture, boolean redirect) {
                    if (!frame.isMain() || !request.isRedirect(navigation.getURL())) return false;
                    try { code.complete(request.authorizationCode(navigation.getURL())); }
                    catch (Exception ex) { code.completeExceptionally(ex); }
                    return true; // Consume the OAuth redirect; never render its authorization code.
                }
            });
            client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                @Override public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String url, String name) {
                    // Keep Microsoft's account and verification pages in the same application window.
                    if (url.startsWith("https://")) browser.loadURL(url);
                    return true;
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                if (code.isDone()) return;
                var browser = client.createBrowser(request.authorizationUrl(), false, false);
                var window = frameHolder[0];
                window.getContentPane().removeAll();
                window.add(browser.getUIComponent(), BorderLayout.CENTER);
                window.add(status, BorderLayout.SOUTH);
                window.revalidate();
                window.repaint();
                window.toFront();
            });
            return awaitToken(code);
        } catch (CancellationException ex) {
            throw ex;
        } catch (IOException | InterruptedException | TimeoutException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Could not initialize the Microsoft sign-in window");
        } finally {
            if (app != null) {
                app.dispose();
                try { terminated.get(5, TimeUnit.SECONDS); }
                catch (ExecutionException | TimeoutException ignored) {}
            }
            SwingUtilities.invokeLater(() -> { if (frameHolder[0] != null) frameHolder[0].dispose(); });
        }
    }

    private MsaToken awaitToken(CompletableFuture<String> code) throws IOException, InterruptedException, TimeoutException {
        try {
            return httpClient.executeAndHandle(request.tokenRequest(code.get(5, TimeUnit.MINUTES)));
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof CancellationException cancelled) throw cancelled;
            throw new IOException("Microsoft sign-in could not complete");
        }
    }
}
