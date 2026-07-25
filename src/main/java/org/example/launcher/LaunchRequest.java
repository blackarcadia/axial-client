package org.example.launcher;

import java.nio.file.Path;
import java.util.UUID;

public class LaunchRequest {
    private final String versionId;
    private final Path gameDir;
    private final String playerName;
    private final UUID playerUuid;
    private final String accessToken;
    private final String xuid;

    private LaunchRequest(Builder builder) {
        this.versionId = builder.versionId;
        this.gameDir = builder.gameDir;
        this.playerName = builder.playerName;
        this.playerUuid = builder.playerUuid != null
                ? builder.playerUuid
                : UUID.nameUUIDFromBytes(builder.playerName.getBytes());
        this.accessToken = builder.accessToken;
        this.xuid = builder.xuid;
    }

    public String getVersionId() {
        return versionId;
    }

    public Path getGameDir() {
        return gameDir;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getXuid() {
        return xuid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String versionId;
        private Path gameDir;
        private String playerName = "Player";
        private UUID playerUuid;
        private String accessToken = "0";
        private String xuid = "0";

        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        public Builder playerName(String playerName) {
            this.playerName = playerName;
            return this;
        }

        public Builder playerUuid(UUID playerUuid) {
            this.playerUuid = playerUuid;
            return this;
        }

        public Builder gameDir(Path gameDir) {
            this.gameDir = gameDir;
            return this;
        }

        public Builder gameDirDefault() {
            String home = System.getProperty("user.home");
            this.gameDir = Path.of(home, ".minecraft");
            return this;
        }

        public Builder accessToken(String token) {
            this.accessToken = token;
            return this;
        }

        public Builder xuid(String xuid) {
            this.xuid = xuid;
            return this;
        }

        public LaunchRequest build() {
            if (versionId == null || versionId.isBlank()) {
                throw new IllegalArgumentException("versionId is required");
            }
            if (gameDir == null) {
                gameDirDefault();
            }
            return new LaunchRequest(this);
        }
    }
}
