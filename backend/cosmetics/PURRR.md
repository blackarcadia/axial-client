# Purrr cape

ID: `purrr`. Display name: **Purrr**. Category: Capes. Static, using the supplied
artwork without recolouring, resizing or a generated shimmer.

Grant it at https://api.axialclient.com/admin: load a Minecraft UUID, check Purrr,
and save cosmetic access. No default player grants or store mappings are added.
The updated client is needed to display/equip the cape; ownership refreshes
through the existing cosmetics API.

Source faces live in `assets/purrr`: `back.png` (outward), `front.png` (inward),
`side.png` (both sides), and `top.png` (top and bottom). The supplied attachment
order is used: plain purple pattern outward, cat/PURRR artwork inward.

Run `java -Djava.awt.headless=true tools/GeneratePurrrCape.java` to pack the
2048 × 1024 texture and regenerate the admin thumbnail. The code uses the same
vanilla cape UVs as existing capes and copies every source pixel unchanged.
Purrr has one texture frame; existing animated capes retain their 16-frame loops.
