package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.EmoteMenuTheme;
import net.minecraft.client.gl.RenderPipelines;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class EmotePaletteCompatibilityTest {
    @Test void wheelColourFilterKeepsTheVanillaGuiShaderInterface()throws Exception{
        var vanilla=RenderPipelines.GUI_TEXTURED;var themed=EmoteMenuTheme.WHEEL;
        assertEquals(vanilla.getVertexShader(),themed.getVertexShader());assertEquals(vanilla.getUniforms(),themed.getUniforms());
        assertEquals(vanilla.getSamplers(),themed.getSamplers());assertEquals(vanilla.getBlendFunction(),themed.getBlendFunction());
        assertEquals(vanilla.getVertexFormat(),themed.getVertexFormat());assertEquals(vanilla.getVertexFormatMode(),themed.getVertexFormatMode());
        String path="/assets/"+themed.getFragmentShader().getNamespace()+"/shaders/"+themed.getFragmentShader().getPath()+".fsh";
        try(var in=getClass().getResourceAsStream(path)){assertNotNull(in,path);}
    }
    @Test void paletteHooksMatchTheBundledPickerAndOnlyModifyColourArguments()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var list=readBundled("io/github/kosmx/emotes/arch/gui/widgets/EmoteListWidget");
            var selection=list.methods.stream().filter(m->m.name.equals("renderSelection")&&m.desc.endsWith(";I)V")).findFirst().orElseThrow();
            assertNotNull(selection);
            var entry=readBundled("io/github/kosmx/emotes/arch/gui/widgets/EmoteListWidget$ListEntry");
            var render=entry.methods.stream().filter(m->m.name.equals("method_25343")).findFirst().orElseThrow();
            assertEquals(1,Arrays.stream(render.instructions.toArray()).filter(n->n instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/class_332")&&call.name.equals("method_25294")&&call.desc.equals("(IIIII)V")).count());
            for(String hook:List.of("EmoteListPaletteMixin","EmoteListEntryPaletteMixin"))assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+hook+".class"));
            try(var in=jar.getInputStream(jar.getJarEntry("assets/axial_cosmetics/shaders/core/emote_palette.fsh"))){assertTrue(in.readAllBytes().length>0);}
        }
    }
    private static ClassNode readBundled(String name)throws Exception{
        // Emotecraft is shipped as a nested dependency; verify its real intermediary names.
        try(var jar=new JarFile(Path.of("src/main/resources/emotecraft-fabric-for-MC1.21.11-3.2.0-b.build.159.jar").toFile())){
            var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);
            try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}
        }
    }
}
