# Korgie

Cosmetic ID: `korgie`. PayNow product: `628277311203966976`.
Bodywear/Outfits category, Boots slot. Equipping Korgie replaces other shoes.

The supplied Bedrock model retains 16 cubes, both leg anchors, fractional
rotations, inflation and authored face UVs. Its 16×16 logical UV canvas uses the
supplied 32×32 texture unchanged. The existing footwear renderer follows each
animated leg and respects invisibility, spectator and item-skin conflicts.

The VPS catalog and purchase mapping are deployed. Synthetic signed purchase
and refund checks cover this product. PayNow returns Korgie as visible, tagged
`axial-client-cosmetics`, and enabled for one-time purchase.

The neighbouring `axialclientwebsite/dist` contains the Bodywear card mapping,
texture, thumbnail and native 3D mesh. Upload that folder's contents to publish
the website changes. The client must also be updated to render the cosmetic.
Live Minecraft visual testing and a real checkout remain untested.

Export with `tools/ExportKorgie.java` on the client runtime classpath to
`/private/tmp/korgie.json`; run `python3 tools/RenderKorgiePreview.py` for thumbnails.
