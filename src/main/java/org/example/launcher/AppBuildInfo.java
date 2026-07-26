package org.example.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppBuildInfo {
    private final String appVersion;
    private final String appChannel;
    private final String githubOwner;
    private final String githubRepo;
    private final String githubAsset;

    private AppBuildInfo(String appVersion, String appChannel, String githubOwner, String githubRepo, String githubAsset) {
        this.appVersion = appVersion;
        this.appChannel = appChannel;
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
        this.githubAsset = githubAsset;
    }

    public static AppBuildInfo load() {
        Properties properties = new Properties();
        try (InputStream in = AppBuildInfo.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ignored) {
        }

        return new AppBuildInfo(
                properties.getProperty("app.version", "0.0.0"),
                properties.getProperty("app.channel", "dev"),
                properties.getProperty("github.owner", "blackarcadia"),
                properties.getProperty("github.repo", "axial-client"),
                properties.getProperty("github.asset", "AxialLauncher.app.zip")
        );
    }

    public String appVersion() {
        return appVersion;
    }

    public String appChannel() {
        return appChannel;
    }

    public String githubOwner() {
        return githubOwner;
    }

    public String githubRepo() {
        return githubRepo;
    }

    public String githubAsset() {
        return githubAsset;
    }
}
