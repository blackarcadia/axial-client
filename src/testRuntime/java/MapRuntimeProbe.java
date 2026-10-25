import java.net.URLClassLoader;
import java.nio.file.Path;

/** Exercise the packaged map allocator without Minecraft, a GPU, or jdk.management. */
public final class MapRuntimeProbe {
    public static void main(String[] args) throws Exception {
        if (ModuleLayer.boot().findModule("jdk.management").isPresent())
            throw new IllegalStateException("Probe must run without jdk.management");
        try (var loader = new URLClassLoader(new java.net.URL[]{Path.of(args[0]).toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> texture = loader.loadClass("net.minecraft.class_1043");
            Class<?> mixin = loader.loadClass("com.axial.cosmetics.mixin.ImmediatelyFastMapRuntimeCompatibilityMixin");
            var allocator = mixin.getDeclaredMethod("axial$createMapPlaceholder", Class.class);
            allocator.setAccessible(true);
            Object result = allocator.invoke(null, texture);
            if (!texture.isInstance(result)) throw new IllegalStateException("Wrong placeholder type");
            if (texture.getField("width").getInt(result) != 1 || texture.getField("height").getInt(result) != 1)
                throw new IllegalStateException("Placeholder must be 1x1");
            System.out.println("Map runtime fallback passed without jdk.management or Reflect.Objects");
        }
    }
}
