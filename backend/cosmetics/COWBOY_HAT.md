# Cowboy Hat

Cosmetic ID: `cowboy_hat`. Category: Headwear. Slot: Head.
PayNow product: `628258556944064512`.

The supplied 20-cube Bedrock geometry and 64×64 texture are preserved. The hat
follows the animated player head. It supports wardrobe previews, server-granted
ownership and the standard head slot conflict rules. Authored face UVs use the
existing Sodium-compatible renderer.

The VPS catalog and PayNow mapping are deployed. Synthetic signed purchases and
refunds cover this product. Client hat and wardrobe tests and website preview
checks pass. Live Minecraft visual validation and a real checkout remain untested.

The website source is in the neighbouring `axialclientwebsite` checkout. Its
Headwear category and explicit product mapping are included in
`output/cowboy-hat-store-update.zip`; upload those paths to axialclient.com.
PayNow must expose this product with the `axial-client-cosmetics` tag. At the
time of implementation the public tagged catalog did not return this product.

Export previews using `tools/ExportCowboyHat.java` on the main runtime classpath,
writing `/private/tmp/cowboy_hat.json`, then `python3 tools/RenderCowboyHatPreview.py`.
