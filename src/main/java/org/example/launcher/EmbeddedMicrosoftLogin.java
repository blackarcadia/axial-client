package org.example.launcher;

import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.data.MsaEnvironment;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

/** Keeps Chromium's UI and native libraries separate from Minecraft's GLFW process. */
public final class EmbeddedMicrosoftLogin {
    private EmbeddedMicrosoftLogin() {}

    public static void main(String[] args) {
        int exitCode = 1;
        try {
            if (args.length != 1) throw new IllegalArgumentException("Missing result file");
            var config = new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH)
                    .withRedirectUri(MsaEnvironment.LIVE.getNativeClientUrl());
            var auth = JavaAuthManager.create(MinecraftAuth.createHttpClient("AxialLauncher/1.0"))
                    .msaApplicationConfig(config).login((http, appConfig) ->
                            new ChromiumMicrosoftAuthService(http, appConfig, Path.of(args[0] + ".browser")));
            auth.getMinecraftToken().getUpToDate();
            auth.getMinecraftProfile().getUpToDate();
            Files.writeString(Path.of(args[0]), JavaAuthManager.toJson(auth).toString());
            exitCode = 0;
        } catch (CancellationException ex) {
            exitCode = 2;
        } catch (Throwable ex) {
            // OAuth responses can contain credentials; log the failure type only.
            System.err.println("Microsoft sign-in failed (" + ex.getClass().getSimpleName() + ").");
        } finally {
            System.exit(exitCode);
        }
    }
}
