package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class UiTypographyTest {
    @Test void brandingIsUppercaseRegardlessOfSystemLanguage() {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("MINING ASSISTANCE",UiTypography.uppercase("Mining assistance"));
            assertEquals("ESC TO CLOSE",UiTypography.uppercase("Esc to close"));
        } finally { Locale.setDefault(previous); }
    }

    @Test void menuHelpersUppercaseDisplayedLabelsWithoutChangingTheirSourceValues() throws Exception {
        String savedName="My waypoint";
        assertEquals("MY WAYPOINT",ClientMenuText.literal(savedName).getString());
        assertEquals("My waypoint",savedName);
        assertEquals("GLINT STRENGTH",NavigationScreen.copy("Glint Strength",.9f,true).getString());
        var settings=ClientSettingsScreen.class.getDeclaredMethod("styled",String.class,float.class,boolean.class);
        settings.setAccessible(true);
        assertEquals("SHOW SHADOWS BENEATH PLAYERS",((net.minecraft.text.Text)settings.invoke(null,"Show shadows beneath players",.8f,false)).getString());
    }

    @Test void allUsedFontSizesHaveNativeRasterProviders() throws Exception {
        for(float scale:new float[]{.7f,.75f,.78f,.8f,.85f,.9f,.95f,1f,1.1f,1.2f,1.35f,1.8f}) {
            for(boolean bold:new boolean[]{false,true}) {
                var id=UiTypography.font(scale,bold);
                String path="/assets/"+id.getNamespace()+"/font/"+id.getPath()+".json";
                try(var stream=UiTypographyTest.class.getResourceAsStream(path)) {
                    assertNotNull(stream,"Missing native font: "+path);
                    var provider=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonArray("providers").get(0).getAsJsonObject();
                    assertEquals(UiTypography.size(scale),provider.get("size").getAsInt());
                    assertTrue(provider.get("oversample").getAsDouble()>=4);
                    String file=provider.get("file").getAsString().split(":")[1];
                    assertNotNull(UiTypographyTest.class.getResource("/assets/axial_cosmetics/font/"+file));
                }
            }
        }
    }
}
