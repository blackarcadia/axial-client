package com.axial.cosmetics.client;

import com.axial.cosmetics.mixin.LivingEntityRenderStateSmartMiningMixin;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.axial.axialutils.client.ContrabandHighlightState;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class MiningFeatureRenderingTest {
    private static final List<String> HOOKS=List.of("LivingEntityRenderStateSmartMiningMixin","LivingEntityRendererSmartMiningMixin","DrawContextContrabandMixin");
    @Test void restorationHooksAreRegisteredInTheActualPackagedMod() throws Exception {
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var config=JsonParser.parseString(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var active=new HashSet<String>();config.getAsJsonArray("client").forEach(v->active.add(v.getAsString()));
            for(String hook:HOOKS){assertTrue(active.contains(hook),hook+" must be active, not merely included in the JAR");assertNotNull(jar.getJarEntry("com/axial/cosmetics/mixin/"+hook+".class"));}
        }
    }
    @Test void everyRenderInjectionMatchesTheCurrentMinecraftMethod() throws Exception {
        for(String hook:HOOKS) {
            var mixin=read("com/axial/cosmetics/mixin/"+hook);
            var annotation=mixin.invisibleAnnotations.stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
            @SuppressWarnings("unchecked") var targetTypes=(List<Type>)value(annotation,"value");
            var target=read(targetTypes.getFirst().getInternalName());
            int checked=0;
            for(var method:mixin.methods) {
                var annotations=new ArrayList<AnnotationNode>();
                if(method.visibleAnnotations!=null)annotations.addAll(method.visibleAnnotations);
                if(method.invisibleAnnotations!=null)annotations.addAll(method.invisibleAnnotations);
                for(var a:annotations)if(a.desc.endsWith("/Inject;")||a.desc.endsWith("/ModifyReturnValue;")) {
                    @SuppressWarnings("unchecked") var selectors=(List<String>)value(a,"method");
                    for(String selector:selectors) {
                        assertTrue(target.methods.stream().anyMatch(m->selector.equals(m.name)||selector.equals(m.name+m.desc)),hook+": "+selector);
                        checked++;
                    }
                }
            }
            if(!hook.contains("StateSmart"))assertTrue(checked>0);
        }
    }
    @Test void miningFadePreservesTintAndAlreadyReducedAlpha() {
        assertEquals(0x1AFFFFFF,SmartMiningVisuals.color(0xFFFFFFFF,true));
        assertEquals(0x0D80FF00,SmartMiningVisuals.color(0x8080FF00,true));
        assertEquals(0x0080FF00,SmartMiningVisuals.color(0x0080FF00,true));
        assertEquals(0x8080FF00,SmartMiningVisuals.color(0x8080FF00,false));
    }
    @Test void reusedRenderStateCanReturnToOpaque() {
        class State extends LivingEntityRenderStateSmartMiningMixin { }
        var state=new State();assertFalse(state.axialutils$shouldFade());
        state.axialutils$setShouldFade(true);assertTrue(state.axialutils$shouldFade());
        state.axialutils$setShouldFade(false);assertFalse(state.axialutils$shouldFade());
    }
    @Test void contrabandParserStillRecognisesAllServerTiersAndNestedBukkitData() throws Exception {
        int[] colors={1149272192,1140915968,1157627648,1157597696};
        for(int tier=1;tier<=4;tier++) {
            var values=new NbtCompound();values.putInt("prisonscore:contraband-tier-level",tier);
            assertEquals(colors[tier-1],overlay(values));
            var root=new NbtCompound();root.put("PublicBukkitValues",values);
            assertEquals(colors[tier-1],overlay(root));
            var nested=new NbtList();nested.add(root);var wrapper=new NbtCompound();wrapper.put("data",nested);
            assertEquals(colors[tier-1],overlay(wrapper));
        }
        var basic=new NbtCompound();basic.putBoolean("prisonscore:contraband",true);
        assertEquals(colors[0],overlay(basic));assertEquals(0,overlay(new NbtCompound()));
    }
    private static int overlay(NbtCompound data) throws Exception {
        var method=ContrabandHighlightState.class.getDeclaredMethod("findContrabandOverlayColor",NbtCompound.class);method.setAccessible(true);return (int)method.invoke(null,data);
    }
    private static Object value(AnnotationNode a,String key){int i=a.values.indexOf(key);assertTrue(i>=0);return a.values.get(i+1);}
    private static ClassNode read(String name) throws Exception {
        try(var stream=MiningFeatureRenderingTest.class.getResourceAsStream("/"+name+".class")){assertNotNull(stream,name);var node=new ClassNode();new ClassReader(stream).accept(node,0);return node;}
    }
}
