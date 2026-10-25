package com.axial.cosmetics.client;

/** Reprojects an already-uploaded world raster into the current viewport every frame. */
record MapTextureProjection(int left,int top,int right,int bottom,float u1,float u2,float v1,float v2) {
    static MapTextureProjection raster(MapViewport view,double centerX,double centerZ,double scale,int left,int top,int size){
        double span=scale*256;
        return worldRect(view,centerX-span/2,centerZ-span/2,span,span,left,top,size,1,1);
    }
    static MapTextureProjection worldRect(MapViewport view,double minX,double minZ,double width,double height,
                                         int left,int top,int size,float maxU,float maxV){
        double x1=view.pixelX(minX,left,size),y1=view.pixelZ(minZ,top,size);
        double x2=view.pixelX(minX+width,left,size),y2=view.pixelZ(minZ+height,top,size);
        if(!Double.isFinite(x1)||!Double.isFinite(y1)||!Double.isFinite(x2)||!Double.isFinite(y2)||x2<=x1||y2<=y1)return null;
        // Clip before converting to integer GUI coordinates. Extreme zoom can otherwise
        // submit enormous quads and lose UV precision even though the map is scissored.
        int l=(int)Math.round(Math.clamp(x1,left,left+size)),t=(int)Math.round(Math.clamp(y1,top,top+size));
        int r=(int)Math.round(Math.clamp(x2,left,left+size)),b=(int)Math.round(Math.clamp(y2,top,top+size));
        if(r<=l||b<=t)return null;
        return new MapTextureProjection(l,t,r,b,
                (float)Math.clamp((l-x1)/(x2-x1)*maxU,0,maxU),(float)Math.clamp((r-x1)/(x2-x1)*maxU,0,maxU),
                (float)Math.clamp((t-y1)/(y2-y1)*maxV,0,maxV),(float)Math.clamp((b-y1)/(y2-y1)*maxV,0,maxV));
    }
}
