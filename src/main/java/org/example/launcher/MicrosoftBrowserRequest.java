package org.example.launcher;

import net.lenni0451.commons.httpclient.content.impl.URLEncodedFormContent;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

/** OAuth request state belongs to one window and is never written to logs or saved accounts. */
final class MicrosoftBrowserRequest {
    private final MsaApplicationConfig config;
    private final String state = randomValue();
    private final String verifier = randomValue();

    MicrosoftBrowserRequest(MsaApplicationConfig config) {
        this.config = config.withRedirectUri(config.getEnvironment().getNativeClientUrl());
    }

    MsaApplicationConfig config() { return config; }

    String authorizationUrl() {
        var parameters = new LinkedHashMap<>(config.getAuthCodeParameters());
        parameters.put("prompt", "select_account");
        parameters.put("state", state);
        parameters.put("code_challenge_method", "S256");
        try {
            parameters.put("code_challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        return config.getEnvironment().getAuthorizeUrl() + "?" + parameters.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
    }

    boolean isRedirect(String url) {
        try {
            URI actual = URI.create(url), expected = URI.create(config.getRedirectUri());
            return expected.getScheme().equalsIgnoreCase(actual.getScheme())
                    && expected.getHost().equalsIgnoreCase(actual.getHost())
                    && expected.getPath().equals(actual.getPath())
                    && (actual.getPort() == -1 || actual.getPort() == 443)
                    && actual.getUserInfo() == null;
        } catch (IllegalArgumentException ex) { return false; }
    }

    String authorizationCode(String url) throws IOException {
        if (!isRedirect(url)) throw new IOException("Unexpected sign-in redirect");
        var parameters = new HashMap<String, String>();
        String query = URI.create(url).getRawQuery();
        if (query == null) throw new IOException("Missing sign-in response");
        try {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                if (parameters.putIfAbsent(key, value) != null) throw new IOException("Duplicate sign-in parameter");
            }
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid sign-in response"); }
        if (!state.equals(parameters.get("state"))) throw new IOException("Sign-in response did not match this window");
        if ("access_denied".equals(parameters.get("error"))) throw new CancellationException("Sign-in cancelled");
        if (parameters.containsKey("error")) throw new IOException("Microsoft could not complete sign-in");
        String code = parameters.get("code");
        if (code == null || code.isBlank()) throw new IOException("Missing authorization code");
        return code;
    }

    MsaAuthCodeTokenRequest tokenRequest(String code) throws IOException {
        var request = new MsaAuthCodeTokenRequest(config, code);
        var form = new HashMap<String, String>();
        form.put("client_id", config.getClientId());
        form.put("scope", config.getScope());
        form.put("redirect_uri", config.getRedirectUri());
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("code_verifier", verifier);
        request.setContent(new URLEncodedFormContent(form));
        return request;
    }

    private static String randomValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
