package org.example.launcher;

public class AuthResult {
    public final String playerName;
    public final String uuid; // without dashes
    public final String accessToken;
    public final String xuid;

    AuthResult(String playerName, String uuid, String accessToken, String xuid) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.xuid = xuid;
    }
}
