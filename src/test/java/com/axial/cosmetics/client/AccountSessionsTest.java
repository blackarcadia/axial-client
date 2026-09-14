package com.axial.cosmetics.client;

import com.mojang.authlib.minecraft.UserApiService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountSessionsTest {
    @Test void signedOutSessionHasNoCredentialsProfileOrOnlinePermissions() {
        var signedOut = AccountSessions.signedOut();
        assertTrue(signedOut.session().getAccessToken().isEmpty());
        assertTrue(signedOut.session().getXuid().isEmpty());
        assertTrue(signedOut.session().getClientId().isEmpty());
        assertSame(UserApiService.OFFLINE, signedOut.service());
        assertTrue(signedOut.properties().flags().isEmpty());
        assertNull(signedOut.profile());
    }
}
