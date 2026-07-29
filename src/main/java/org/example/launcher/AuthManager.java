package org.example.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AuthManager {
    private final Path storeFile;
    private final HttpClient httpClient;
    private MicrosoftSignInWindow signInWindow;

    public AuthManager(Path storeFile) {
        this.storeFile = storeFile;
        this.httpClient = MinecraftAuth.createHttpClient("AxialLauncher/1.0");
    }

    public AuthResult authenticate() throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        try {
            JavaAuthManager authManager;
            if (Files.exists(storeFile)) {
                JsonObject json = JsonParser.parseString(Files.readString(storeFile)).getAsJsonObject();
                authManager = JavaAuthManager.fromJson(httpClient, json);
            } else {
                ParamMsaAuthServiceSupplier<java.util.function.Consumer<MsaDeviceCode>> supplier =
                        (client, appConfig, consumer) -> new DeviceCodeMsaAuthService(client, appConfig, consumer);
                authManager = JavaAuthManager.create(httpClient).login(supplier, this::showDeviceCodeUi);
                persist(authManager);
            }

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
        } finally {
            closeSignInWindow();
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

    private void showDeviceCodeUi(MsaDeviceCode code) {
        if (signInWindow == null) {
            signInWindow = MicrosoftSignInWindow.show(
                    code.getVerificationUri(),
                    code.getDirectVerificationUri(),
                    code.getUserCode()
            );
        } else {
            signInWindow.update(
                    code.getVerificationUri(),
                    code.getDirectVerificationUri(),
                    code.getUserCode(),
                    "Use the code shown below to sign in."
            );
        }
    }

    private void closeSignInWindow() {
        if (signInWindow != null) {
            signInWindow.close();
            signInWindow = null;
        }
    }
}
