package com.axial.cosmetics.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Executes the bundled addColorRows bytecode without launching Minecraft.
 * Only rendering and config callbacks are stubbed; group branching and enum
 * field resolution are real, including the missing RUPTURE field.
 */
public class ColorRowsRegressionTest implements Opcodes {
    private static final String SCREEN = "org/axial/axialutils/client/AxialConfigScreen";
    private static final String GROUP = SCREEN + "$ColorGroup";
    private static final String HANDLER = "axial_cosmetics$skipRemovedRuptureColorGroup";
    private static final Map<String, List<String>> EXPECTED = Map.of(
            "HUD", List.of("INFO TITLE", "CHARGE / MIN", "XP / MIN"),
            "SATCHEL", List.of("SATCHEL TITLE", "SATCHEL COUNT", "SATCHEL EMPTY"),
            "CPS", List.of("TITLE", "CPS"),
            "ARMOR", List.of("ARMOR", "DURABILITY"));

    @Test
    void originalLibraryReproducesReportedCrashForAllThreeGroups() throws Exception {
        for (String group : List.of("SATCHEL", "CPS", "ARMOR")) {
            var failure = assertThrows(InvocationTargetException.class, () -> runRows(group, false));
            assertInstanceOf(NoSuchFieldError.class, failure.getCause());
            assertTrue(failure.getCause().getMessage().contains("RUPTURE"));
        }
    }

    @Test
    void redirectedLayoutBuildsEveryExpectedColorRow() throws Exception {
        for (var entry : EXPECTED.entrySet()) {
            assertEquals(entry.getValue(), runRows(entry.getKey(), true));
        }
    }

    private List<?> runRows(String groupName, boolean patched) throws Exception {
        try (JarFile jar = new JarFile(Path.of("src/main/resources/axialutils-1.0-SNAPSHOT.jar").toFile())) {
            ClassNode original = read(jar.getInputStream(jar.getJarEntry(SCREEN + ".class")).readAllBytes());
            MethodNode layout = original.methods.stream().filter(m -> m.name.equals("addColorRows")).findFirst().orElseThrow();
            MethodNode row = original.methods.stream().filter(m -> m.name.equals("addColorRow")).findFirst().orElseThrow();
            ClassNode fixture = new ClassNode();
            fixture.version = V21;
            fixture.access = ACC_PUBLIC;
            fixture.name = SCREEN;
            fixture.superName = "java/lang/Object";
            fixture.fields.add(new FieldNode(ACC_PUBLIC, "panelWidth", "I", null, null));
            fixture.fields.add(new FieldNode(ACC_PUBLIC, "rows", "Ljava/util/List;", null, null));
            MethodNode ctor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
            ctor.instructions.add(new VarInsnNode(ALOAD, 0));
            ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            ctor.instructions.add(new InsnNode(RETURN));
            fixture.methods.add(ctor);

            MethodNode handler;
            try (var in = getClass().getResourceAsStream("/com/axial/cosmetics/mixin/AxialConfigScreenColorPickerMixin.class")) {
                assertNotNull(in);
                handler = read(in.readAllBytes()).methods.stream().filter(m -> m.name.equals(HANDLER)).findFirst().orElseThrow();
            }
            // Check that the production redirect targets this exact obsolete field.
            AnnotationNode redirect = handler.visibleAnnotations.stream()
                    .filter(a -> a.desc.endsWith("/Redirect;")).findFirst().orElseThrow();
            AnnotationNode at = (AnnotationNode) annotationValue(redirect, "at");
            assertEquals("addColorRows", ((List<?>) annotationValue(redirect, "method")).getFirst());
            assertEquals("L" + GROUP + ";RUPTURE:L" + GROUP + ";", annotationValue(at, "target"));
            fixture.methods.add(handler);

            int redirects = 0;
            for (AbstractInsnNode insn : layout.instructions.toArray()) {
                if (insn instanceof InvokeDynamicInsnNode) {
                    // Color callbacks are irrelevant to layout and require a live config.
                    layout.instructions.set(insn, new InsnNode(ACONST_NULL));
                } else if (patched && insn instanceof FieldInsnNode field
                        && field.owner.equals(GROUP) && field.name.equals("RUPTURE")) {
                    layout.instructions.insertBefore(insn, new MethodInsnNode(INVOKESTATIC, SCREEN, HANDLER, "()Ljava/lang/Object;", false));
                    layout.instructions.set(insn, new TypeInsnNode(CHECKCAST, GROUP));
                    redirects++;
                }
            }
            if (patched) assertEquals(1, redirects);
            fixture.methods.add(layout);
            // Record the labels produced by the real branch, returning the usual row spacing.
            row.instructions.clear();
            row.tryCatchBlocks.clear();
            row.localVariables = null;
            row.instructions.add(new VarInsnNode(ALOAD, 0));
            row.instructions.add(new FieldInsnNode(GETFIELD, SCREEN, "rows", "Ljava/util/List;"));
            row.instructions.add(new VarInsnNode(ALOAD, 6));
            row.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true));
            row.instructions.add(new InsnNode(POP));
            row.instructions.add(new VarInsnNode(ILOAD, 2));
            row.instructions.add(new IntInsnNode(BIPUSH, 28));
            row.instructions.add(new InsnNode(IADD));
            row.instructions.add(new InsnNode(IRETURN));
            fixture.methods.add(row);
            // Existing frames remain valid; replacement callbacks keep their stack shape.
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            fixture.accept(writer);

            class FixtureLoader extends ClassLoader {
                FixtureLoader() { super(null); }
                @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                    try {
                        byte[] bytes = name.equals(SCREEN.replace('/', '.')) ? writer.toByteArray()
                                : jar.getInputStream(jar.getJarEntry(name.replace('.', '/') + ".class")).readAllBytes();
                        return defineClass(name, bytes, 0, bytes.length);
                    } catch (Exception e) { throw new ClassNotFoundException(name, e); }
                }
            }
            ClassLoader loader = new FixtureLoader();
            Class<?> screenClass = loader.loadClass(SCREEN.replace('/', '.'));
            Class<?> groupClass = loader.loadClass(GROUP.replace('/', '.'));
            Object screen = screenClass.getConstructor().newInstance();
            screenClass.getField("panelWidth").setInt(screen, 360);
            List<String> rows = new ArrayList<>();
            screenClass.getField("rows").set(screen, rows);
            var group = groupClass.getDeclaredField(groupName);
            group.setAccessible(true);
            var method = screenClass.getDeclaredMethod("addColorRows", int.class, int.class, groupClass);
            method.setAccessible(true);
            assertEquals(50 + EXPECTED.get(groupName).size() * 28, method.invoke(screen, 10, 50, group.get(null)));
            return rows;
        }
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static Object annotationValue(AnnotationNode node, String key) {
        return node.values.get(node.values.indexOf(key) + 1);
    }
}
