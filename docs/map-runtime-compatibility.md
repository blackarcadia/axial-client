# Filled-map compatibility on older launcher runtimes

Receiving a map (including a clue scroll) can crash an older Axial launcher with:

```
NoClassDefFoundError: com/sun/management/HotSpotDiagnosticMXBean
net.lenni0451.reflect.Objects.<clinit>
net.minecraft.client.texture.MapTextureManager$MapTexture.<clinit>
```

ImmediatelyFast 1.14.3 allocates its shared atlas placeholder with `Reflect.Objects.allocate`. That helper initializes JVM diagnostics, which require `jdk.management`. Older trimmed launcher images omitted that module.

New launcher runtime images already include `jdk.management`, and `verifyRuntimeImage` checks that the diagnostics bean is present before packaging. For existing launchers, `ImmediatelyFastMapRuntimeCompatibilityMixin` replaces only the placeholder allocation with a regular 1x1 `NativeImageBackedTexture`. The atlas and clue-scroll images remain enabled. The compatibility mixin uses priority 1100, above ImmediatelyFast's 1000. Mixin merges legacy static initializers before preparing legacy injections, and rejects an INVOKE redirect into a method merged by an equal or higher-priority mixin. The previous priority 900 caused an InvalidInjectionException as soon as a filled map loaded on either platform. The redirect remains optional when the map atlas is disabled or ImmediatelyFast is absent.

A child JVM restricted to `java.base` executes the actual packaged allocator against a GPU-free texture constructor fixture, confirming it works without `jdk.management` or Reflect.Objects. The packaged-bytecode regression test also checks the pinned third-party allocation, remapped target, redirect priority, optional injection, and tiny texture constructor. An isolated actual-Mixin integration test reproduces the priority-900 failure using ImmediatelyFast's pinned initializer, verifies that priority 1100 redirects it, executes the transformed initializer with a GPU-free texture fixture, and checks the atlas-disabled case. Run:

```
./gradlew :test --tests '*ImmediatelyFastMapRuntimeCompatibilityTest' --tests '*Iris*'
```

A client restart is needed to load an updated mod. On an older launcher, the manual workaround is `map_atlas_generation: false` in `config/immediatelyfast.json`; installing the patched client removes the need for that workaround. In-game verification should include receiving a filled map with atlas generation enabled, holding it, opening inventory, and reconnecting. Also check atlas-disabled startup.
