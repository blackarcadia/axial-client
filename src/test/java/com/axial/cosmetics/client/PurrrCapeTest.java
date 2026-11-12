package com.axial.cosmetics.client;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PurrrCapeTest {
    @Test void staticCapeIsRecognizedButRequiresAnExplicitGrant() throws Exception {
        assertSame(WardrobeCape.PURRR,WardrobeCape.fromId("purrr"));
        assertFalse(WardrobeCape.PURRR.ownedBy(CreatorCosmetics.OWNER));
        assertFalse(WardrobeCape.PURRR.ownedBy(new UUID(0,1)));
        for(int frame=0;frame<16;frame++)assertEquals(WardrobeCape.PURRR.texture(0),WardrobeCape.PURRR.texture(frame*5));
        var uuid=UUID.fromString("12345678-1234-4234-8234-123456789012");
        var grants=CosmeticAssignments.parse("{\"version\":1,\"players\":{\""+uuid+"\":[\"purrr\"]}}");
        assertEquals(Set.of("purrr"),grants.get(uuid));
    }
    @Test void packagedAtlasPreservesAllSixSuppliedFacesExactly() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var entry=jar.getJarEntry("assets/axial_cosmetics/textures/entity/purrr_cape.png");assertNotNull(entry);
            try(var input=jar.getInputStream(entry)) {
                var atlas=ImageIO.read(input);assertEquals(2048,atlas.getWidth());assertEquals(1024,atlas.getHeight());
                String[] names={"back","front","side","side","top","top"};
                int[] us={1,12,0,11,1,11},vs={1,1,1,1,0,0};
                for(int face=0;face<names.length;face++) {
                    var source=ImageIO.read(Path.of("assets/purrr/"+names[face]+".png").toFile());
                    int[] expected=source.getRGB(0,0,source.getWidth(),source.getHeight(),null,0,source.getWidth());
                    int[] actual=atlas.getRGB(us[face]*32,vs[face]*32,source.getWidth(),source.getHeight(),null,0,source.getWidth());
                    assertArrayEquals(expected,actual,names[face]+" face");
                }
            }
        }
    }
}
