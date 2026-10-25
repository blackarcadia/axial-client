package com.axial.cosmetics.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerUpdateImageTest {
    @Test void boundsDecodedImageSizeAndPreservesAspectRatio() throws Exception {
        var source=new BufferedImage(1920,618,BufferedImage.TYPE_INT_RGB);
        var bytes=new ByteArrayOutputStream();ImageIO.write(source,"png",bytes);
        var image=ServerUpdateImage.decode(bytes.toByteArray());
        assertEquals(512,image.width());assertEquals(165,image.height());
        assertEquals(image.width()*image.height(),image.argb().length);
    }
    @Test void retainsArgbColorsAndDoesNotEnlargeSmallImages() throws Exception {
        var source=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0,0,0xFFFF0000);source.setRGB(1,0,0x8000FF00);
        var bytes=new ByteArrayOutputStream();ImageIO.write(source,"png",bytes);
        var image=ServerUpdateImage.decode(bytes.toByteArray());
        assertEquals(2,image.width());assertEquals(1,image.height());
        assertArrayEquals(new int[]{0xFFFF0000,0x8000FF00},image.argb());
    }
    @Test void unsupportedImageFailsWithoutCreatingATexture() {
        assertThrows(IOException.class,()->ServerUpdateImage.decode(new byte[]{1,2,3}));
    }
}
