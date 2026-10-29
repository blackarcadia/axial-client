package com.axial.cosmetics.client;

import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

class WardrobeTest {
    @Test void accessRequiresOwnerSessionAndMatchingWorldPlayer() {
        UUID owner=CreatorCosmetics.OWNER, stranger=new UUID(0,1);
        assertTrue(WardrobeAccess.allows(owner,null));
        assertTrue(WardrobeAccess.allows(owner,owner));
        assertFalse(WardrobeAccess.allows(stranger,null));
        assertFalse(WardrobeAccess.allows(stranger,stranger));
        assertFalse(WardrobeAccess.allows(stranger,owner));
        assertFalse(WardrobeAccess.allows(owner,stranger));
        assertFalse(WardrobeAccess.allows(null,null));
        assertFalse(WardrobeAccess.allows(null,owner));
    }
    @Test void panelsCardsAndActionsFitAcrossGuiScales() {
        for(int w:new int[]{260,320,427,640,960,1280})for(int h:new int[]{165,210,270,360,540,720}) {
            var l=new WardrobeLayout(w,h);
            inside(l.panel,new Rect(0,0,w,h));
            for(Rect r:new Rect[]{l.close,l.categories,l.search,l.grid,l.preview,l.details,l.done})inside(r,l.panel);
            for(int i=0;i<6;i++)inside(l.category(i),l.categories);
            for(int i=0;i<2;i++)inside(l.card(i),l.grid);
            assertTrue(l.search.right()<l.preview.x());
            assertTrue(l.details.bottom()<=l.done.y());
            assertTrue(l.grid.right()<l.preview.x());
        }
    }
    @Test void capeArtworkFitsInsideTheTileWithoutBeingStretched() {
        for(int w:new int[]{260,320,427,640,960,1280})for(int h:new int[]{165,210,270,360,540,720}) {
            var layout=new WardrobeLayout(w,h);
            for(int i=0;i<2;i++) {
                Rect card=layout.card(i),image=WardrobeLayout.capeImage(card);
                inside(image,card);
                assertTrue(Math.abs(image.height()-image.width()*1.6)<1.1);
                if(card.height()>=55)assertTrue(image.bottom()<card.bottom()-29,"artwork must not cover the caption");
            }
        }
    }
    @Test void wardrobeBypassesGenericButtonSkinAndCosmeticsRenderAsIndependentTiles() throws Exception {
        var skin=read("/com/axial/cosmetics/mixin/InGameButtonSkinMixin.class");
        boolean excluded=false;
        for(var method:skin.methods)for(var instruction:method.instructions) {
            if(instruction instanceof TypeInsnNode type && type.getOpcode()==Opcodes.INSTANCEOF
                    &&type.desc.equals("com/axial/cosmetics/client/WardrobeScreen")) {
                assertTrue(type.getNext() instanceof JumpInsnNode);
                var branch=(JumpInsnNode)type.getNext();
                assertEquals(Opcodes.IFNE,branch.getOpcode());
                AbstractInsnNode target=branch.label.getNext();
                while(target!=null&&target.getOpcode()<0)target=target.getNext();
                assertNotNull(target);
                assertEquals(Opcodes.RETURN,target.getOpcode(),"wardrobe must return before the generic skin cancels custom rendering");
                excluded=true;
            }
        }
        assertTrue(excluded,"wardrobe must be excluded from the generic skin");
        var tile=read("/com/axial/cosmetics/client/WardrobeScreen$CosmeticCard.class");
        assertEquals("net/minecraft/client/gui/widget/ClickableWidget",tile.superName);
        assertTrue(tile.methods.stream().anyMatch(m->m.name.equals("keyPressed")));
        boolean drawsCape=false;
        for(var method:tile.methods)for(var instruction:method.instructions)
            if(instruction instanceof MethodInsnNode call&&call.name.equals("drawCape"))drawsCape=true;
        assertTrue(drawsCape,"the cosmetic tile must render real cape artwork");
    }
    private static ClassNode read(String resource) throws Exception {
        try(var input=WardrobeTest.class.getResourceAsStream(resource)) {
            assertNotNull(input);var node=new ClassNode();new ClassReader(input.readAllBytes()).accept(node,0);return node;
        }
    }
    private static void inside(Rect child,Rect parent) {
        assertTrue(child.width()>0&&child.height()>0,child.toString());
        assertTrue(child.x()>=parent.x()&&child.y()>=parent.y(),child.toString());
        assertTrue(child.right()<=parent.right()&&child.bottom()<=parent.bottom(),child+" outside "+parent);
    }
}
