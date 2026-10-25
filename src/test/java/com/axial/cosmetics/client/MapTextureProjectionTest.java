package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapTextureProjectionTest {
    @Test void unchangedRasterExactlyFillsTheViewport(){
        var view=new MapViewport(100,-500,2);var p=MapTextureProjection.raster(view,100,-500,2,20,40,512);
        assertNotNull(p);assertEquals(20,p.left());assertEquals(40,p.top());assertEquals(532,p.right());assertEquals(552,p.bottom());
        assertEquals(0,p.u1());assertEquals(1,p.u2());assertEquals(0,p.v1());assertEquals(1,p.v2());
    }
    @Test void cachedTerrainAndOverlaysStayAlignedThroughoutDraggingBeforeAnyNewUpload(){
        var view=new MapViewport(-1200,800,2);double rasterX=view.centerX,rasterZ=view.centerZ,scale=view.scale;
        for(int frame=0;frame<40;frame++){
            view.pan(3,-2,512);
            var p=MapTextureProjection.raster(view,rasterX,rasterZ,scale,91,37,512);assertNotNull(p);
            assertAligned(p,view,rasterX+40,rasterZ-30,rasterX-256,rasterZ-256,512,512,91,37,512,1,1);
        }
    }
    @Test void aDelayedRasterFromAnOlderZoomStillUsesTheCurrentProjection(){
        var view=new MapViewport(10,20,2);view.zoom(3,210,280,40,60,500);view.pan(17,-9,500);
        var p=MapTextureProjection.raster(view,10,20,2,40,60,500);assertNotNull(p);
        assertAligned(p,view,view.centerX,view.centerZ,-246,-236,512,512,40,60,500,1,1);
    }
    @Test void partialOverviewTexturesPreserveTheirUvRange(){
        var view=new MapViewport(0,0,1);var p=MapTextureProjection.worldRect(view,-128,-128,256,256,0,0,256,.5f,.75f);
        assertNotNull(p);assertEquals(.5f,p.u2());assertEquals(.75f,p.v2());
        assertAligned(p,view,0,0,-128,-128,256,256,0,0,256,.5f,.75f);
    }
    @Test void offscreenTexturesDisappearAndExtremeZoomQuadsStayBounded(){
        var view=new MapViewport(0,0,.25);assertNull(MapTextureProjection.raster(view,30000000,30000000,1,10,20,400));
        var p=MapTextureProjection.raster(view,0,0,MapViewport.MAX_SCALE,10,20,400);assertNotNull(p);
        assertEquals(10,p.left());assertEquals(410,p.right());assertEquals(20,p.top());assertEquals(420,p.bottom());
        assertTrue(Float.isFinite(p.u1()));assertTrue(p.u1()>=0&&p.u2()<=1);assertTrue(p.v1()>=0&&p.v2()<=1);
    }
    private static void assertAligned(MapTextureProjection p,MapViewport view,double worldX,double worldZ,
                                      double minX,double minZ,double width,double height,int left,int top,int size,float maxU,float maxV){
        double u=(worldX-minX)/width*maxU,v=(worldZ-minZ)/height*maxV;
        double textureX=p.left()+(u-p.u1())/(p.u2()-p.u1())*(p.right()-p.left());
        double textureY=p.top()+(v-p.v1())/(p.v2()-p.v1())*(p.bottom()-p.top());
        assertEquals(view.pixelX(worldX,left,size),textureX,.001);assertEquals(view.pixelZ(worldZ,top,size),textureY,.001);
    }
}
