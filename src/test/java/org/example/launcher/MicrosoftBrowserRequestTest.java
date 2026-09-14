package org.example.launcher;

import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;

class MicrosoftBrowserRequestTest {
    private MicrosoftBrowserRequest request() {
        return new MicrosoftBrowserRequest(new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH));
    }

    @Test void requestsMicrosoftLoginWithNativeRedirectAndPkce() {
        var login = request();
        var uri = URI.create(login.authorizationUrl());
        assertEquals("https", uri.getScheme());
        assertEquals("login.live.com", uri.getHost());
        var parameters = query(uri);
        assertEquals(MsaConstants.JAVA_TITLE_ID, parameters.get("client_id"));
        assertEquals("https://login.live.com/oauth20_desktop.srf", parameters.get("redirect_uri"));
        assertEquals("select_account", parameters.get("prompt"));
        assertEquals("S256", parameters.get("code_challenge_method"));
        assertEquals(43, parameters.get("code_challenge").length());
        assertNotEquals(parameters.get("state"), query(URI.create(request().authorizationUrl())).get("state"));
    }

    @Test void acceptsOnlyMatchingMicrosoftCallbackForThisLogin() throws Exception {
        var login = request();
        String state = query(URI.create(login.authorizationUrl())).get("state");
        String callback = login.config().getRedirectUri();
        assertEquals("test-code", login.authorizationCode(callback + "?state=" + state + "&code=test-code"));
        assertThrows(IOException.class, () -> login.authorizationCode(callback + "?state=wrong&code=test-code"));
        assertThrows(IOException.class, () -> login.authorizationCode(callback + "?code=test-code"));
        assertFalse(login.isRedirect("https://example.com/?code=test-code&state=" + state));
        assertFalse(login.isRedirect("https://login.live.com.example.com/oauth20_desktop.srf"));
        assertFalse(login.isRedirect("http://login.live.com/oauth20_desktop.srf"));
        assertFalse(login.isRedirect("https://login.live.com:444/oauth20_desktop.srf"));
        assertFalse(login.isRedirect("https://login.live.com/another-path"));
    }

    @Test void tokenExchangeUsesTheVerifierForThisWindowsChallenge() throws Exception {
        var login = request();
        var authorization = query(URI.create(login.authorizationUrl()));
        var body = login.tokenRequest("test-code").getContent().getAsString();
        var token = query(URI.create("https://example.invalid/?" + body));
        String challenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(
                        token.get("code_verifier").getBytes(StandardCharsets.US_ASCII)));
        assertEquals(authorization.get("code_challenge"), challenge);
        assertEquals(authorization.get("redirect_uri"), token.get("redirect_uri"));
        assertEquals("test-code", token.get("code"));
        assertEquals("authorization_code", token.get("grant_type"));
    }

    @Test void rejectsDuplicateParametersAndHandlesUserCancellation() {
        var login = request();
        String callback = login.config().getRedirectUri();
        String state = query(URI.create(login.authorizationUrl())).get("state");
        assertThrows(IOException.class, () -> login.authorizationCode(callback + "?state=" + state + "&state=" + state + "&code=x"));
        assertThrows(CancellationException.class, () -> login.authorizationCode(callback + "?state=" + state + "&error=access_denied"));
        assertThrows(IOException.class, () -> login.authorizationCode(callback + "?state=" + state + "&error=server_error"));
    }

    private static Map<String, String> query(URI uri) {
        var parameters = new HashMap<String, String>();
        for (String part : uri.getRawQuery().split("&")) {
            var pair = part.split("=", 2);
            parameters.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return parameters;
    }
}
