package com.axial.cosmetics.client;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ModFeaturePreviewsTest {
    @Test void hudPreviewsCallTheSameRenderersAsGameplayInsteadOfRecreatingTheirText() throws Exception {
        ClassNode node=new ClassNode();
        try(var in=getClass().getResourceAsStream("/com/axial/cosmetics/client/ModFeaturePreviews.class")) {
            assertNotNull(in);new ClassReader(in.readAllBytes()).accept(node,0);
        }
        Set<String> renderers=new HashSet<>();
        for(var method:node.methods)for(var instruction:method.instructions) {
            if(instruction instanceof MethodInsnNode call&&(call.name.equals("render")||call.name.equals("renderPreview")))renderers.add(call.owner);
            if(instruction instanceof LdcInsnNode value&&value.cst instanceof String text) {
                assertFalse(text.startsWith("SATCHELS: 3"));
                assertFalse(text.startsWith("PING: 24"));
                assertNotEquals("NW     N     NE",text);
                assertNotEquals("SPEED II   1:42",text);
                assertNotEquals("8 CPS",text);
            }
        }
        for(String renderer:new String[]{"ArmorHudRenderer","AxialHudRenderer","CpsHudRenderer","SatchelHudRenderer"})
            assertTrue(renderers.contains("org/axial/axialutils/client/"+renderer),renderer);
        assertTrue(renderers.contains("com/axial/cosmetics/client/CompassHudRenderer"));
        assertTrue(renderers.contains("com/axial/cosmetics/client/PotionsHudRenderer"));
    }
}
