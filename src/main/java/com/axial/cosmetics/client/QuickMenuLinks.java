package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

final class QuickMenuLinks {
    private QuickMenuLinks() {}

    static URI get(String name) {
        try(var stream=QuickMenuLinks.class.getResourceAsStream("/assets/axial_cosmetics/quick-menu-links.json")) {
            if(stream==null)return null;
            var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            return parse(json.get(name).getAsString());
        } catch(Exception ex) { return null; }
    }

    static URI parse(String value) {
        if(value==null||value.isBlank())return null;
        try {
            URI uri=URI.create(value.strip());
            return ("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))
                    &&uri.getHost()!=null&&uri.getUserInfo()==null?uri:null;
        } catch(IllegalArgumentException ex) { return null; }
    }
}
