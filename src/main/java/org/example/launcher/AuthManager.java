package org.example.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.service.impl.JfxWebViewMsaAuthService;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AuthManager {
    private final Path storeFile;
    private final HttpClient httpClient;
    private static final Object FX_LOCK = new Object();
    private static volatile boolean fxStarted;

    public AuthManager(Path storeFile) {
        this.storeFile = storeFile;
        this.httpClient = MinecraftAuth.createHttpClient("AxialLauncher/1.0");
    }

    public AuthResult authenticate() throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        ensureJavaFx();
        JavaAuthManager loadedAuth = loadStoredAuth();
        if (loadedAuth == null) {
            loadedAuth = JavaAuthManager.create(httpClient)
                    .login(JfxWebViewMsaAuthService::new);
            persist(loadedAuth);
        }
        final JavaAuthManager authManager = loadedAuth;

        // persist on any token update
        authManager.getChangeListeners().add(() -> {
            try {
                persist(authManager);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        String accessToken = authManager.getMinecraftToken().getUpToDate().getToken();
        var profile = authManager.getMinecraftProfile().getUpToDate();
        String name = profile.getName();
        String uuid = profile.getId().toString();

        String xuid = authManager.getJavaXstsToken().getUpToDate().getUserHash();

        return new AuthResult(name, uuid, accessToken, xuid);
    }

    private static void ensureJavaFx() {
        if (fxStarted) {
            return;
        }
        synchronized (FX_LOCK) {
            if (fxStarted) {
                return;
            }
            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException ignored) {
            }
            fxStarted = true;
        }
    }

    private JavaAuthManager loadStoredAuth() throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        if (!Files.exists(storeFile)) {
            return null;
        }

        try {
            JsonObject json = JsonParser.parseString(Files.readString(storeFile)).getAsJsonObject();
            JavaAuthManager authManager = JavaAuthManager.fromJson(httpClient, json);
            authManager.getMsaToken().refreshIfExpired();
            authManager.getJavaXstsToken().refreshIfExpired();
            authManager.getMinecraftToken().refreshIfExpired();
            authManager.getMinecraftProfile().getUpToDate();
            persist(authManager);
            return authManager;
        } catch (Exception ex) {
            Files.deleteIfExists(storeFile);
            return null;
        }
    }

    private void persist(JavaAuthManager authManager) throws IOException {
        JsonObject obj = JavaAuthManager.toJson(authManager);
        Files.writeString(storeFile, obj.toString(), StandardCharsets.UTF_8);
    }

    public static AuthResult peek(Path storeFile) throws IOException {
        if (!Files.exists(storeFile)) return null;
        JsonObject json = JsonParser.parseString(Files.readString(storeFile)).getAsJsonObject();
        JsonObject profile = json.getAsJsonObject("minecraftProfile");
        if (profile == null || !profile.has("name") || !profile.has("id")) return null;
        String name = profile.get("name").getAsString();
        String uuid = profile.get("id").getAsString();
        return new AuthResult(name, uuid, "", "0");
    }

}
