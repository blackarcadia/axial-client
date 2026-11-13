package com.axial.cosmetics.client;

import java.nio.file.Path;
import java.util.UUID;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagentaCityCapeTest {
    @Test void staticCapeRequiresAnExplicitGrant() throws Exception {
        var cape=WardrobeCape.fromId("magenta_city");
        assertSame(WardrobeCape.MAGENTA_CITY,cape);
        assertEquals("Magenta City",cape.label);
        assertFalse(cape.ownedBy(CreatorCosmetics.OWNER));
        assertFalse(cape.ownedBy(new UUID(0,1)));
        assertEquals(cape.texture(0),cape.texture(200));
        var grants=CosmeticAssignments.parse("{\"version\":1,\"players\":{\"12345678-1234-4234-8234-123456789012\":[\"magenta_city\"]}}");
        assertTrue(grants.get(UUID.fromString("12345678-1234-4234-8234-123456789012")).contains(cape.id));
    }
    @Test void packagedCapeHasMatchingFacesAndEdges() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var entry=jar.getJarEntry("assets/axial_cosmetics/textures/entity/magenta_city_cape.png");
            assertNotNull(entry);
            try(var in=jar.getInputStream(entry)) {
                var image=ImageIO.read(in);
                assertEquals(2048,image.getWidth());assertEquals(1024,image.getHeight());
                assertArrayEquals(image.getRGB(32,32,320,512,null,0,320),image.getRGB(384,32,320,512,null,0,320));
                assertArrayEquals(image.getRGB(0,32,32,512,null,0,32),image.getRGB(352,32,32,512,null,0,32));

            }
        }
    }
}
