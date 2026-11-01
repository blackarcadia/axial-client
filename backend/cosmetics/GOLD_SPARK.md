# Gold Spark aura

ID: `gold_spark`. Category: Auras. Grant through the cosmetics admin catalogue after deploying the updated catalogue and assets. No automatic grants or paid products are created. The updated client is required.

Gold sparks rise at staggered, seeded random intervals, with 0.6–1 second lifetimes, varied trail lengths, stepped amber tails, pale gold tips and brief cross-shaped shimmer. The six-second loop has independent per-player phase offsets. Rendering uses the existing emissive, depth-tested aura pipeline, with reduced detail beyond 24 blocks and a 64-block cutoff.

Equip in Wardrobe → Auras. Existing ownership checks and aura equipment synchronization apply. If another aura is already equipped, select Gold Spark explicitly.

`GoldSparkGeometry.java` defines the effect. Regenerate the wardrobe atlas, admin still and looping GIF from the same geometry:

```sh
mkdir -p /tmp/axial-gold-preview
javac -d /tmp/axial-gold-preview src/main/java/com/axial/cosmetics/client/HexwindGeometry.java src/main/java/com/axial/cosmetics/client/GoldSparkGeometry.java tools/GenerateGoldSparkPreview.java
java -Djava.awt.headless=true -cp /tmp/axial-gold-preview GenerateGoldSparkPreview
```
