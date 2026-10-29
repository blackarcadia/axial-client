package com.axial.cosmetics.mixin;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.*;

/** Isolated real-Mixin integration probe. Uses the pinned atlas initializer and GPU-free target/texture fixtures. */
public final class MapMixinTransformProbe {
    public static void main(String[] args) throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        if(Boolean.parseBoolean(args[1]))Mixins.addConfiguration("map-upstream-probe.json");
        Mixins.addConfiguration("map-atlas-probe.json");
        // Match Fabric Loader's decorations for the pinned mods' minimum loader versions:
        // Axial >=0.16.9 -> Mixin 0.14.0; ImmediatelyFast >=0.18.0 -> Mixin 0.16.5.
        for(var config:Mixins.getConfigs())config.getConfig().decorate("fabric-compat",
                config.getName().equals("map-atlas-probe.json")?14000:16005);
        var constructor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();
        constructor.setAccessible(true);
        var transformer=(IMixinTransformer)constructor.newInstance();
        String name="net.minecraft.class_10093$class_331";
        byte[] original=Files.readAllBytes(Path.of(args[0],name.replace('.','/')+".class"));
        byte[] transformed=transformer.transformClassBytes(name,name,original);
        var node=new ClassNode();new ClassReader(transformed).accept(node,0);
        boolean enabled=Boolean.parseBoolean(args[1]);int calls=0;
        System.out.println("Transformed methods: "+node.methods.stream().map(m->m.name).toList());
        for(var method:node.methods)if(method.name.equals("<clinit>"))for(var instruction:method.instructions)
            if(instruction instanceof MethodInsnNode call) {
                if(call.owner.equals("net/lenni0451/reflect/Objects"))throw new AssertionError("Reflect allocator survived transformation");
                if(call.name.contains("createMapPlaceholder"))calls++;
            }
        if(calls!=(enabled?1:0))throw new AssertionError("Placeholder redirect count: "+calls);
        if(enabled) {
            var loader=new ClassLoader(ClassLoader.getPlatformClassLoader()) {
                @Override protected Class<?> findClass(String type) throws ClassNotFoundException {
                    try {
                        byte[] bytes=type.equals(name)?transformed:Files.readAllBytes(Path.of(args[0],type.replace('.','/')+".class"));
                        return defineClass(type,bytes,0,bytes.length);
                    }catch(IOException ex){throw new ClassNotFoundException(type,ex);}
                }
            };
            Class<?> target=Class.forName(name,true,loader);
            var field=target.getDeclaredField("DUMMY_TEXTURE");field.setAccessible(true);Object texture=field.get(null);
            if(texture.getClass().getField("width").getInt(texture)!=1||texture.getClass().getField("height").getInt(texture)!=1)
                throw new AssertionError("Transformed initializer did not create the tiny fallback");
        }
        System.out.println("Actual Mixin transformation passed; atlas="+enabled);
    }

    public static final class Service extends MixinServiceAbstract implements IClassProvider,IClassBytecodeProvider {
        public String getName(){return "Axial map regression probe";}
        public boolean isValid(){return true;}
        @Override public MixinEnvironment.Phase getInitialPhase(){return MixinEnvironment.Phase.DEFAULT;}
        public IClassProvider getClassProvider(){return this;}
        public IClassBytecodeProvider getBytecodeProvider(){return this;}
        public ITransformerProvider getTransformerProvider(){return null;}
        public IClassTracker getClassTracker(){return null;}
        public IMixinAuditTrail getAuditTrail(){return null;}
        public IFeatureValidator getFeatureValidator(){return IFeatureValidator.ALLOW_ALL;}
        public IAdviceProvider getAdviceProvider(){return IAdviceProvider.GENERIC;}
        public Collection<String> getPlatformAgents(){return List.of();}
        public IContainerHandle getPrimaryContainer(){return new ContainerHandleVirtual("map-probe");}
        @Override public Collection<IContainerHandle> getMixinContainers(){return List.of();}
        public InputStream getResourceAsStream(String name){return getClass().getClassLoader().getResourceAsStream(name);}
        public URL[] getClassPath(){return new URL[0];}
        public Class<?> findClass(String name)throws ClassNotFoundException{return findClass(name,false);}
        public Class<?> findClass(String name,boolean initialize)throws ClassNotFoundException{return Class.forName(name,initialize,getClass().getClassLoader());}
        public Class<?> findAgentClass(String name,boolean initialize)throws ClassNotFoundException{return findClass(name,initialize);}
        public ClassNode getClassNode(String name)throws IOException,ClassNotFoundException{return getClassNode(name,false,0);}
        public ClassNode getClassNode(String name,boolean transform)throws IOException,ClassNotFoundException{return getClassNode(name,transform,0);}
        public ClassNode getClassNode(String name,boolean transform,int flags)throws IOException,ClassNotFoundException{
            try(var in=getResourceAsStream(name.replace('.','/')+".class")){
                if(in==null)throw new ClassNotFoundException(name);
                var node=new ClassNode();new ClassReader(in).accept(node,flags);return node;
            }
        }
    }
    public static final class Properties implements IGlobalPropertyService {
        private record Key(String name) implements IPropertyKey {}
        private final Map<IPropertyKey,Object> values=new HashMap<>();
        public IPropertyKey resolveKey(String name){return new Key(name);}
        @SuppressWarnings("unchecked") public <T>T getProperty(IPropertyKey key){return (T)values.get(key);}
        public void setProperty(IPropertyKey key,Object value){values.put(key,value);}
        public <T>T getProperty(IPropertyKey key,T fallback){T value=getProperty(key);return value==null?fallback:value;}
        public String getPropertyString(IPropertyKey key,String fallback){Object value=values.get(key);return value==null?fallback:value.toString();}
    }
}
