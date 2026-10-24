package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class IrisShadowRuntimeCompatibilityTest {
    @Test void pinnedIrisInitializerOnlyInitializesTheOptionalFmaFlag()throws Exception{
        try(var jar=new JarFile("src/main/resources/iris-fabric-1.10.8+mc1.21.11.jar")){
            var iris=read(jar,"net/irisshaders/iris/shadows/frustum/advanced/AdvancedShadowCullingFrustum");
            var field=iris.fields.stream().filter(f->f.name.equals("FMA_SUPPORT")).findFirst().orElseThrow();assertEquals("Z",field.desc);assertNull(field.value);
            var init=iris.methods.stream().filter(m->m.name.equals("<clinit>")).findFirst().orElseThrow();
            boolean diagnostics=false,flag=false;
            for(var instruction:init.instructions){
                if(instruction instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC){assertEquals("FMA_SUPPORT",f.name,"Review new Iris static initialization before replacing it");assertEquals(iris.name,f.owner);flag=true;}
                if(instruction instanceof LdcInsnNode constant&&constant.cst instanceof Type type&&type.getClassName().equals("com.sun.management.HotSpotDiagnosticMXBean"))diagnostics=true;
            }
            assertTrue(diagnostics);assertTrue(flag);
            boolean fallback=false;
            for(var m:iris.methods)for(var instruction:m.instructions)if(instruction instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETSTATIC&&f.name.equals("FMA_SUPPORT")){
                AbstractInsnNode next=instruction.getNext();while(next.getOpcode()<0)next=next.getNext();assertEquals(Opcodes.IFEQ,next.getOpcode());fallback=true;
            }
            assertTrue(fallback,"Iris must retain its own non-FMA rendering branch");
        }
    }
    @Test void packagedMixinAssignsFlagBeforeCancelingTheUnsafeInitializer()throws Exception{
        try(var jar=new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())){
            var mixin=read(jar,"com/axial/cosmetics/mixin/IrisShadowRuntimeCompatibilityMixin");
            var handler=mixin.methods.stream().filter(m->m.name.equals("axial$optionalHotSpotDiagnostics")).findFirst().orElseThrow();
            var inject=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/Inject;")).findFirst().orElseThrow();
            assertEquals(List.of("<clinit>"),value(inject,"method"));assertEquals(Boolean.TRUE,value(inject,"cancellable"));
            var at=(AnnotationNode)((List<?>)value(inject,"at")).getFirst();assertEquals("HEAD",value(at,"value"));
            int index=0,helper=-1,assignment=-1,cancel=-1;
            for(var n:handler.instructions){
                if(n instanceof MethodInsnNode call&&call.owner.endsWith("/IrisShaderCompatibility")&&call.name.equals("fmaEnabled"))helper=index;
                if(n instanceof FieldInsnNode f&&f.name.equals("FMA_SUPPORT")&&f.getOpcode()==Opcodes.PUTSTATIC)assignment=index;
                if(n instanceof MethodInsnNode call&&call.name.equals("cancel"))cancel=index;
                index++;
            }
            assertTrue(helper>=0&&assignment>helper&&cancel>assignment);
            try(var in=jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json"))){assertTrue(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains("IrisShadowRuntimeCompatibilityMixin"));}
        }
    }
    private static Object value(AnnotationNode node,String key){return node.values.get(node.values.indexOf(key)+1);}
    private static ClassNode read(JarFile jar,String name)throws Exception{var entry=jar.getJarEntry(name+".class");assertNotNull(entry,name);try(var in=jar.getInputStream(entry)){var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
