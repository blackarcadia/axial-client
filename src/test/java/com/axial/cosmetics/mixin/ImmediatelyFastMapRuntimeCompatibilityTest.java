package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

class ImmediatelyFastMapRuntimeCompatibilityTest {
    private static final String ALLOCATOR = "net/lenni0451/reflect/Objects";

    @Test void pinnedMapAtlasAllocatesOnlyItsSharedPlaceholderThroughReflect() throws Exception {
        try (var jar = new JarFile("src/main/resources/ImmediatelyFast-Fabric-1.14.3+1.21.11.jar")) {
            var mixin = read(jar, "net/raphimc/immediatelyfast/injection/mixins/map_atlas_generation/MixinMapTextureManager_MapInstance");
            var init = mixin.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElseThrow();
            var allocations = new ArrayList<MethodInsnNode>();
            var assignments = new ArrayList<FieldInsnNode>();
            for (var instruction : init.instructions) {
                if (instruction instanceof MethodInsnNode call) allocations.add(call);
                if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.PUTSTATIC) assignments.add(field);
            }
            assertEquals(1, allocations.size(), "Review changed atlas initialization before intercepting it");
            assertEquals(ALLOCATOR, allocations.getFirst().owner);
            assertEquals("allocate", allocations.getFirst().name);
            assertEquals("(Ljava/lang/Class;)Ljava/lang/Object;", allocations.getFirst().desc);
            assertEquals(1, assignments.size());
            assertEquals("DUMMY_TEXTURE", assignments.getFirst().name);
            assertEquals("Lnet/minecraft/class_1043;", assignments.getFirst().desc);
            // The atlas only uses the placeholder as a return value, never invokes methods on it.
            int reads = 0;
            for (var method : mixin.methods) for (var instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode field && field.name.equals("DUMMY_TEXTURE") && field.getOpcode() == Opcodes.GETSTATIC) {
                    AbstractInsnNode next = instruction.getNext();
                    while (next.getOpcode() < 0) next = next.getNext();
                    assertEquals(Opcodes.ARETURN, next.getOpcode());
                    reads++;
                }
            }
            assertEquals(1, reads);
        }
    }

    @Test void packagedMixinReplacesTheAllocatorAfterAtlasMixinAndStaysOptional() throws Exception {
        try (var jar = new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile())) {
            var mixin = read(jar, "com/axial/cosmetics/mixin/ImmediatelyFastMapRuntimeCompatibilityMixin");
            var target = mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
            assertEquals(900, value(target, "priority"));
            assertEquals(List.of("net/minecraft/class_10093$class_331"), value(target, "targets"));
            var handler = mixin.methods.stream().filter(m -> m.name.equals("axial$createMapPlaceholder")).findFirst().orElseThrow();
            assertEquals("(Ljava/lang/Class;)Ljava/lang/Object;", handler.desc);
            var redirect = handler.visibleAnnotations.stream().filter(a -> a.desc.endsWith("/Redirect;")).findFirst().orElseThrow();
            assertEquals(List.of("<clinit>"), value(redirect, "method"));
            assertEquals(0, value(redirect, "require"), "Must also work with vanilla maps or a disabled atlas");
            var at = (AnnotationNode)((List<?>)value(redirect, "at")).getFirst();
            assertEquals("INVOKE", value(at, "value"));
            assertEquals("L" + ALLOCATOR + ";allocate(Ljava/lang/Class;)Ljava/lang/Object;", value(at, "target"));
            assertEquals(Boolean.FALSE, value(at, "remap"));
            int oneConstants = 0, constructors = 0;
            for (var instruction : handler.instructions) {
                if (instruction.getOpcode() == Opcodes.ICONST_1) oneConstants++;
                if (instruction instanceof MethodInsnNode call) {
                    assertNotEquals(ALLOCATOR, call.owner, "Never initialize Reflect.Objects on map receipt");
                    if (call.owner.equals("net/minecraft/class_1043") && call.name.equals("<init>")) {
                        assertEquals("(Ljava/util/function/Supplier;IIZ)V", call.desc);
                        constructors++;
                    }
                }
            }
            assertEquals(3, oneConstants, "Allocate only a 1x1 placeholder, not a map-sized texture");
            assertEquals(1, constructors);
            try (var in = jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json"))) {
                assertTrue(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("ImmediatelyFastMapRuntimeCompatibilityMixin"));
            }
        }
    }

    @Test void packagedAllocatorRunsWithoutTheMissingManagementModule(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        // A GPU-free constructor fixture lets the actual packaged allocator run in
        // a child JVM with the exact module missing from the crashing launcher.
        var writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "net/minecraft/class_1043", null, "java/lang/Object", null);
        for (String field : List.of("width", "height")) writer.visitField(Opcodes.ACC_PUBLIC, field, "I", null, null).visitEnd();
        var constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/function/Supplier;IIZ)V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        for (int i = 0; i < 2; i++) {
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ILOAD, i + 2);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, "net/minecraft/class_1043", i == 0 ? "width" : "height", "I");
        }
        constructor.visitInsn(Opcodes.RETURN); constructor.visitMaxs(2, 5); constructor.visitEnd(); writer.visitEnd();
        Path texture = directory.resolve("net/minecraft/class_1043.class");
        java.nio.file.Files.createDirectories(texture.getParent());
        java.nio.file.Files.write(texture, writer.toByteArray());
        String mixin = "com/axial/cosmetics/mixin/ImmediatelyFastMapRuntimeCompatibilityMixin.class";
        try (var jar = new JarFile(Path.of(System.getProperty("axial.test.modJar")).toFile()); var in = jar.getInputStream(jar.getJarEntry(mixin))) {
            Path target = directory.resolve(mixin); java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Files.write(target, in.readAllBytes());
        }
        String javaCommand = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        var process = new ProcessBuilder(javaCommand, "--limit-modules=java.base", "-cp", System.getProperty("axial.test.runtimeProbeClasses"),
                "MapRuntimeProbe", directory.toString()).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("Map runtime fallback passed"), output);
        } finally { process.destroyForcibly(); }
    }

    private static Object value(AnnotationNode node, String key) { return node.values.get(node.values.indexOf(key) + 1); }
    private static ClassNode read(JarFile jar, String name) throws Exception {
        var entry = jar.getJarEntry(name + ".class");
        assertNotNull(entry, name);
        try (var in = jar.getInputStream(entry)) { var node = new ClassNode(); new ClassReader(in).accept(node, 0); return node; }
    }
}
