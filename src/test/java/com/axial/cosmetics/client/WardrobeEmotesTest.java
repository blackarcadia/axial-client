package com.axial.cosmetics.client;

import io.github.kosmx.emotes.server.serializer.type.impl.BinaryFormat;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardrobeEmotesTest {
    final Map<java.lang.reflect.Field,Object> saved=new HashMap<>();
    @org.junit.jupiter.api.BeforeEach void rememberGrants()throws Exception {
        for(String name:List.of("grants","redeemedAt","minimumRevision")){var field=CosmeticAssignments.class.getDeclaredField(name);field.setAccessible(true);saved.put(field,field.get(null));}
    }
    @org.junit.jupiter.api.AfterEach void restoreGrants()throws Exception {for(var entry:saved.entrySet())entry.getKey().set(null,entry.getValue());EmotePermissionState.reset();}

    @Test void vpsGrantsUnlockEmotesAndTheyAppearInBothWardrobeCollections()throws Exception {
        var account=UUID.randomUUID();String json="{\"version\":1,\"players\":{\""+account+"\":[\"emote_rat_dance\",\"unknown_emote\"]}}";
        var parsed=CosmeticAssignments.parse(json);assertEquals(Set.of("emote_rat_dance"),parsed.get(account));
        CosmeticAssignments.acceptRedemption(account,parsed.get(account),1);
        assertEquals(List.of(WardrobeItem.of(WardrobeEmote.RAT_DANCE)),WardrobeItem.owned(account,WardrobeCategory.EMOTES));
        assertTrue(WardrobeItem.owned(account,WardrobeCategory.ALL).contains(WardrobeItem.of(WardrobeEmote.RAT_DANCE)));
        assertFalse(WardrobeEmote.DEATH.ownedBy(account));assertFalse(WardrobeItem.of(WardrobeEmote.RAT_DANCE).equipped());
    }
    @Test void vpsRevocationsAndServerRestrictionsBothApplyToSavedWheelEmotes() {
        var account=UUID.randomUUID();var e=WardrobeEmote.RAT_DANCE;
        CosmeticAssignments.acceptRedemption(account,Set.of(e.id),1);
        assertTrue(EmotePermissionState.canUse(e.animation,account));
        EmotePermissionState.update("");assertFalse(EmotePermissionState.canUse(e.animation,account));
        EmotePermissionState.update("*");assertTrue(EmotePermissionState.canUse(e.animation,account));
        CosmeticAssignments.acceptRedemption(account,Set.of(),2);assertFalse(EmotePermissionState.canUse(e.animation,account));
    }
    @Test void wheelEditsOnlyTheSelectedNativeSlotAndPreservesOtherPages() {
        var pages=new UUID[15][8];var old=UUID.randomUUID();pages[2][3]=old;var next=EmoteWheelLoadout.copy(pages);
        for(int slot=0;slot<8;slot++)EmoteWheelLoadout.put(next,1,slot,WardrobeEmote.RAT_DANCE.animation);
        assertEquals(old,next[2][3]);assertNull(pages[1][0]);
        EmoteWheelLoadout.put(next,1,4,null);assertNull(next[1][4]);assertNotNull(next[1][3]);
        assertThrows(IllegalArgumentException.class,()->EmoteWheelLoadout.put(next,15,0,old));assertThrows(IllegalArgumentException.class,()->EmoteWheelLoadout.put(next,1,8,old));
    }
    @Test void eightSpatialSlotsFitWithoutOverlappingAtSmallAndLargeGuiSizes() {
        for(int width:new int[]{304,380,470})for(int height:new int[]{224,270,330}) {
            var panel=new ModMenuLayout.Rect(8,8,width,height);var slots=new ArrayList<ModMenuLayout.Rect>();
            for(int i=0;i<8;i++) {
                var r=EmoteWheelLoadout.slotBounds(panel,i);assertTrue(r.x()>=panel.x()+12&&r.right()<=panel.right()-12);
                assertTrue(r.y()>=panel.y()+73&&r.bottom()<panel.bottom()-47);
                for(var prior:slots)assertFalse(r.x()<prior.right()&&r.right()>prior.x()&&r.y()<prior.bottom()&&r.bottom()>prior.y());slots.add(r);
            }
            assertTrue(slots.get(0).y()>slots.get(4).y());assertTrue(slots.get(2).x()>slots.get(6).x());
        }
    }
    @Test void hostedAssetsMatchCatalogChecksumsAndStableNativeWheelIdentities()throws Exception {
        var root=com.google.gson.JsonParser.parseString(Files.readString(Path.of("backend/cosmetics/public/assets/emotes/catalog.json"))).getAsJsonObject();
        assertEquals(6,root.getAsJsonArray("emotes").size());
        var admin=com.google.gson.JsonParser.parseString(Files.readString(Path.of("backend/cosmetics/catalog.json"))).getAsJsonObject();
        for(var value:root.getAsJsonArray("emotes")) {
            var entry=value.getAsJsonObject();var e=WardrobeEmote.fromId(entry.get("id").getAsString());assertNotNull(e);
            assertEquals("Emotes",admin.getAsJsonObject(e.id).get("category").getAsString());
            assertTrue(Files.isRegularFile(Path.of("backend/cosmetics/public/assets",admin.getAsJsonObject(e.id).get("image").getAsString())));
            var data=Files.readAllBytes(Path.of("backend/cosmetics/public/assets/emotes",entry.get("file").getAsString()));
            VpsEmoteAssets.verify(data,entry.get("sha256").getAsString());
            var animation=new BinaryFormat().read(new java.io.ByteArrayInputStream(data),e.file).values().iterator().next();assertEquals(e.animation,animation.uuid());
            assertThrows(java.io.IOException.class,()->VpsEmoteAssets.verify(new byte[]{1,2,3},entry.get("sha256").getAsString()));
        }
    }
    @Test void nativePlaybackEntryAndPackagedOwnershipGuardMatch()throws Exception {
        try(var jar=new java.util.jar.JarFile("src/main/resources/emotecraft-fabric-for-MC1.21.11-3.2.0-b.build.159.jar")) {
            var type=new org.objectweb.asm.tree.ClassNode();
            try(var input=jar.getInputStream(jar.getJarEntry("io/github/kosmx/emotes/main/EmoteHolder.class"))){new org.objectweb.asm.ClassReader(input).accept(type,0);}
            assertTrue(type.methods.stream().anyMatch(m->m.name.equals("playEmote")&&m.desc.equals("()Z")));
        }
        try(var jar=new java.util.jar.JarFile(System.getProperty("axial.test.modJar"))) {
            assertNotNull(jar.getJarEntry("com/axial/cosmetics/client/WardrobeEmotePreview.class"));
            assertNotNull(jar.getJarEntry("com/axial/cosmetics/client/WardrobeEmoteWheelScreen.class"));
            assertNotNull(jar.getJarEntry("com/axial/cosmetics/client/VpsEmoteAssets.class"));
            assertTrue(new String(jar.getInputStream(jar.getJarEntry("axial-cosmetics.mixins.json")).readAllBytes()).contains("EmoteVpsOwnershipMixin"));
        }
    }
    @Test void detachedSpacemanControllerChangesPoseAndRepeatsWithoutAWorldOrLivePlayer()throws Exception {
        for(var emote:WardrobeEmote.values())try(var in=getClass().getResourceAsStream("/emotes/"+emote.file+".emotecraft")) {
            var animation=new BinaryFormat().read(in,emote.file).values().iterator().next();var controller=new WardrobeEmotePreview.PreviewController();controller.triggerAnimation(animation,0);
            controller.sample(0);var first=pose(controller);var poses=new HashSet<List<Float>>();poses.add(first);
            for(int i=1;i<20;i++){controller.sample(animation.length()*i/20);poses.add(pose(controller));}
            assertTrue(poses.size()>1,emote.label+" must visibly animate");
            controller.sample(0);assertEquals(first,pose(controller),emote.label+" repeats/rewinds to the same pose");
        }
    }
    private List<Float> pose(WardrobeEmotePreview.PreviewController controller) {
        var result=new ArrayList<Float>();for(String name:List.of("body","head","torso","right_arm","left_arm","right_leg","left_leg")) {
            var bone=controller.get3DTransform(new PlayerAnimBone(name));result.add(bone.rotX);result.add(bone.rotY);result.add(bone.rotZ);result.add(bone.getPosX());result.add(bone.getPosY());result.add(bone.getPosZ());
        }return result;
    }
}
