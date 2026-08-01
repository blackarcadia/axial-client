package org.example.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;

public class AuthManager {
    private final Path storeFile;
    private final HttpClient httpClient;

    public AuthManager(Path storeFile) {
        this.storeFile = storeFile;
        this.httpClient = MinecraftAuth.createHttpClient("AxialLauncher/1.0");
    }

    public AuthResult authenticate() throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        JavaAuthManager loadedAuth = loadStoredAuth();
        if (loadedAuth == null) {
            loadedAuth = JavaAuthManager.create(httpClient)
                    .login((client, appConfig) -> new DeviceCodeMsaAuthService(client, appConfig, AuthManager::handleDeviceCode));
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

    private static void openBrowser(URL url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(url.toURI());
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            new ProcessBuilder("open", url.toString()).start();
        } catch (IOException ignored) {
        }
    }

    private static void handleDeviceCode(MsaDeviceCode code) {
        try {
            openBrowser(new URL(code.getDirectVerificationUri()));
        } catch (Exception ignored) {
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
