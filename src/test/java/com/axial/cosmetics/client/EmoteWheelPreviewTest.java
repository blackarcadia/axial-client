package com.axial.cosmetics.client;

import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class EmoteWheelPreviewTest {
    @Test void animatedCardsKeepTheExistingEightMedallionCentersAndHoverGrowth() {
        for(int i=0;i<8;i++) {
            double a=Math.PI/2-i*Math.PI/4;int cx=250,cy=250;
            int x=cx+(int)Math.round(Math.cos(a)*180)-50,y=cy+(int)Math.round(Math.sin(a)*180)-50;
            var normal=EmoteWheelPreview.bounds(x,y,100,100,cx,cy,true,false);
            var hover=EmoteWheelPreview.bounds(x,y,100,100,cx,cy,true,true);
            assertEquals(60,normal.width());assertEquals(60,normal.height());assertEquals(66,hover.width());assertEquals(66,hover.height());
            assertEquals(normal.x()+normal.width()/2,hover.x()+hover.width()/2);assertEquals(normal.y()+normal.height()/2,hover.y()+hover.height()/2);
            assertEquals(cx+((x+50)-cx)*.72,normal.x()+30,1);assertEquals(cy+((y+50)-cy)*.72,normal.y()+30,1);
        }
        assertEquals(new ModMenuLayout.Rect(12,34,56,78),EmoteWheelPreview.bounds(12,34,56,78,250,250,false,true));
    }
    @Test void bothNativeIconAndFallbackBranchesMatchTheReplacementHooks()throws Exception {
        try(var jar=new JarFile("src/main/resources/emotecraft-fabric-for-MC1.21.11-3.2.0-b.build.159.jar")) {
            var type=read(jar,"io/github/kosmx/emotes/arch/screen/widget/preview/elemets/PlayerChooseElement");
            var render=type.methods.stream().filter(m->m.name.equals("method_48579")).findFirst().orElseThrow();int icons=0,models=0;
            for(var instruction:render.instructions)if(instruction instanceof MethodInsnNode call) {
                if(call.owner.equals("net/minecraft/class_332")&&call.name.equals("method_25302")&&call.desc.equals("(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/class_2960;IIFFIIIIII)V"))icons++;
                if(call.owner.equals("io/github/kosmx/emotes/arch/gui/widgets/PlayerPreview")&&call.name.equals("method_48579")&&call.desc.equals("(Lnet/minecraft/class_332;IIF)V"))models++;
            }
            assertEquals(1,icons);assertEquals(1,models);
        }
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            assertNotNull(jar.getJarEntry("com/axial/cosmetics/client/EmoteWheelPreview.class"));
            var mixin=read(jar,"com/axial/cosmetics/mixin/EmoteRadialElementThemeMixin");
            for(String method:new String[]{"axial$animateIcon","axial$animateFallback"})assertTrue(mixin.methods.stream().anyMatch(m->m.name.equals(method)));
        }
    }
    private ClassNode read(JarFile jar,String path)throws Exception {
        var type=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(path+".class"))){new ClassReader(in).accept(type,0);}return type;
    }
}
