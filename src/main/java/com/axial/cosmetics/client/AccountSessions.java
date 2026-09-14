package com.axial.cosmetics.client;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.raphimc.minecraftauth.java.JavaAuthManager;

import java.net.Proxy;
import java.util.Optional;

final class AccountSessions {
    static boolean isSignedIn() {
        return !MinecraftClient.getInstance().getSession().getAccessToken().isBlank();
    }

    static Prepared signedOut() {
        Session session = new Session("SignedOut", new java.util.UUID(0, 0), "", Optional.empty(), Optional.empty());
        var properties = new UserApiService.UserProperties(java.util.Set.of(), java.util.Map.of());
        return new Prepared(session, UserApiService.OFFLINE, properties, null);
    }

    record Prepared(Session session, UserApiService service, UserApiService.UserProperties properties, ProfileResult profile) {
        void apply() {
            ((AccountSessionAccess) MinecraftClient.getInstance()).axial$setAccount(session, service, properties, profile);
        }
    }

    static Prepared prepare(JavaAuthManager auth) throws Exception {
        String token = auth.getMinecraftToken().getUpToDate().getToken();
        var profile = auth.getMinecraftProfile().getUpToDate();
        Session session = new Session(profile.getName(), profile.getId(), token, Optional.empty(), Optional.empty());
        UserApiService service = new YggdrasilAuthenticationService(Proxy.NO_PROXY).createUserApiService(token);
        var properties = service.fetchProperties();
        var fullProfile = MinecraftClient.getInstance().getApiServices().sessionService().fetchProfile(profile.getId(), true);
        if (fullProfile == null) throw new IllegalStateException("Minecraft profile unavailable");
        return new Prepared(session, service, properties, fullProfile);
    }
}
