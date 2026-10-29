# Creator cape

`original-logo.png` is the owner's original supplied skull-and-orbit logo,
retained without redrawing or pixel-art conversion. `cape-panel.png` places that
artwork on black fabric with restrained violet trim. The white outer margins
are omitted when packing the supplied logo; its aspect ratio is preserved.

Run `java -Djava.awt.headless=true tools/GenerateCreatorCape.java` from the project
root to rebuild the panel and sixteen shimmer frames under
`src/main/resources/assets/axial_cosmetics/textures/entity/creator_cape*.png`.
Each cape face is 320 × 512 pixels with smooth image resampling.
The client selects one frame every five ticks, giving a four-second loop.
Normal vanilla cape movement and visibility still apply.
