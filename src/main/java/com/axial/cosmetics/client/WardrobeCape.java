package com.axial.cosmetics.client;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

/** Bundled cape catalogue and UUID entitlements. */
enum WardrobeCape {
    GOLD_OWNER("gold_owner", "Gold Owner", "OWNER EXCLUSIVE", "creator_cape",
            "6a9cccc8-8e00-4a73-bd37-1121f81b0674"),
    ADMIN("admin", "Admin", "ADMIN EXCLUSIVE", "admin_cape",
            "6a9cccc8-8e00-4a73-bd37-1121f81b0674",
            "8ef7a848-7c77-4449-92a9-b2fb30b4cfad",
            "a0e00d14-fce1-4ed2-9577-d83a3feedfee"),
    STAFF("staff", "STAFF", "STAFF EXCLUSIVE", "staff_cape",
            "6a9cccc8-8e00-4a73-bd37-1121f81b0674",
            "8ef7a848-7c77-4449-92a9-b2fb30b4cfad",
            "a0e00d14-fce1-4ed2-9577-d83a3feedfee",
            "8d79d555-75e5-4456-bbc4-0268e1d21659");

    final String id, label, badge;
    private final Set<UUID> owners;
    private final List<AssetInfo.TextureAsset> frames;
    WardrobeCape(String id,String label,String badge,String texture,String... uuids) {
        this.id=id;this.label=label;this.badge=badge;
        owners=java.util.Arrays.stream(uuids).map(UUID::fromString).collect(java.util.stream.Collectors.toUnmodifiableSet());
        frames=IntStream.range(0,16).mapToObj(i->(AssetInfo.TextureAsset)new AssetInfo.TextureAssetInfo(
                Identifier.of("axial_cosmetics",texture+"_"+i),Identifier.of("axial_cosmetics","textures/entity/"+texture+(i==0?"":"_"+i)+".png"))).toList();
    }
    boolean ownedBy(UUID uuid) {return uuid!=null&&owners.contains(uuid);}
    static List<WardrobeCape> owned(UUID uuid) {return java.util.Arrays.stream(values()).filter(cape->cape.ownedBy(uuid)).toList();}
    static WardrobeCape defaultFor(UUID uuid) {return owned(uuid).stream().findFirst().orElse(null);}
    static WardrobeCape fromId(String id) {return java.util.Arrays.stream(values()).filter(cape->cape.id.equals(id)).findFirst().orElse(null);}
    AssetInfo.TextureAsset asset(float age) {return frames.get(CreatorCosmetics.frameIndex(age));}
    Identifier texture(float age) {return asset(age).texturePath();}
    boolean hasTexture(SkinTextures textures) {return textures!=null&&textures.cape()!=null&&frames.contains(textures.cape());}
    SkinTextures apply(UUID uuid,SkinTextures original,float age) {
        if(!ownedBy(uuid)||original==null)return original;
        return new SkinTextures(original.body(),asset(age),original.elytra(),original.model(),original.secure());
    }
}
