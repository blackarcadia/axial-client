package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuHeaderLayoutTest {
    @Test void markKeepsItsAspectAndWordmarkSharesItsVerticalCentre() {
        for(int w:new int[]{60,100,164,240,400,680})for(int h:new int[]{20,26,30,32,34}) {
            var area=new Rect(17,23,w,h);var layout=new MenuHeaderLayout(area);
            assertTrue(layout.mark.height()<=18);
            assertTrue(layout.mark.x()>=area.x()&&layout.mark.right()<=area.right());
            assertTrue(layout.mark.y()>=area.y()&&layout.mark.bottom()<=area.bottom());
            assertTrue(Math.abs(layout.mark.width()-layout.mark.height()*512.0/302)<2);
            assertTrue(layout.textX>=layout.mark.right());assertTrue(layout.textX+layout.textWidth<=area.right());
            for(int font:new int[]{7,9,12,16})assertTrue(Math.abs(layout.textY(font)+font/2.0-(layout.mark.y()+layout.mark.height()/2.0))<=.5);
        }
    }
    @Test void newArtworkIsBundledWithItsColoursAndTransparency() throws Exception {
        try(var input=getClass().getResourceAsStream("/assets/axial_cosmetics/textures/gui/branding/menu_mark.png")) {
            assertNotNull(input);var image=ImageIO.read(input);assertEquals(512,image.getWidth());assertEquals(302,image.getHeight());
            assertEquals(0,image.getRGB(0,0)>>>24);
            int purple=0;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int c=image.getRGB(x,y);if((c>>>24)>200&&(c&255)>((c>>8)&255))purple++;
            }
            assertTrue(purple>10000);
        }
    }
    @Test void quickBarStillDrawsItsOriginalLogoAndDoesNotUseTheNewHeader() throws Exception {
        try(var input=getClass().getResourceAsStream("/com/axial/cosmetics/client/QuickMenuScreen.class")) {
            assertNotNull(input);var node=new ClassNode();new ClassReader(input).accept(node,0);boolean original=false;
            for(var method:node.methods)for(var instruction:method.instructions){
                if(instruction instanceof LdcInsnNode ldc&&"textures/gui/menu_logo.png".equals(ldc.cst))original=true;
                if(instruction instanceof MethodInsnNode call)assertNotEquals("com/axial/cosmetics/client/MenuHeader",call.owner);
            }
            assertTrue(original);
        }
    }
}
