import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Blue celestial cape: supplied outward back, generated inward front, unlettered edges.
 * Run: java -Djava.awt.headless=true tools/GenerateLunarCape.java
 */
class GenerateLunarCape {
    private static final Path SOURCE=Path.of("assets/lunar_raven");
    private static BufferedImage face(String name) throws Exception {
        var result=new BufferedImage(320,512,BufferedImage.TYPE_INT_ARGB);
        var graphics=result.createGraphics();
        try {
            graphics.setColor(Color.BLACK);graphics.fillRect(0,0,320,512);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(ImageIO.read(SOURCE.resolve(name).toFile()),0,0,320,512,null);
        } finally {graphics.dispose();}
        return result;
    }
    private static BufferedImage trim(int w,int h) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        boolean vertical=h>w;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int along=vertical?y:x,across=vertical?x:y;
            // Continuous blue seams and sparse angular facets, deliberately no glyphs.
            int color=0xFF060A12;
            if(across==3||across==28)color=0xFF16355E;
            if(across>=13&&across<=18&&along%96>=38&&along%96<58)color=0xFF245B98;
            image.setRGB(x,y,color);
        }
        return image;
    }
    private static void pack(BufferedImage atlas,BufferedImage face,int u,int v,int frame) {
        for(int y=0;y<face.getHeight();y++)for(int x=0;x<face.getWidth();x++) {
            int rgb=face.getRGB(x,y),r=rgb>>>16&255,g=rgb>>>8&255,b=rgb&255;
            double phase=frame*Math.PI*2/16-x*.01125-y*.00625;
            double shimmer=b>50&&b>r*1.3?.10*Math.pow((Math.cos(phase)+1)/2,10):0;
            r+=(int)((100-r)*shimmer);g+=(int)((180-g)*shimmer);b+=(int)((255-b)*shimmer);
            atlas.setRGB(u*32+x,v*32+y,0xFF000000|r<<16|g<<8|b);
        }
    }
    public static void main(String[] args) throws Exception {
        var front=face("front.png");var back=face("back.png");
        var edge=trim(32,512);var end=trim(320,32);
        ImageIO.write(front,"png",SOURCE.resolve("cape-panel.png").toFile());
        ImageIO.write(edge,"png",SOURCE.resolve("left.png").toFile());
        ImageIO.write(edge,"png",SOURCE.resolve("right.png").toFile());
        ImageIO.write(end,"png",SOURCE.resolve("top.png").toFile());
        ImageIO.write(end,"png",SOURCE.resolve("bottom.png").toFile());
        var output=Path.of("src/main/resources/assets/axial_cosmetics/textures/entity");Files.createDirectories(output);
        for(int frame=0;frame<16;frame++) {
            var atlas=new BufferedImage(2048,1024,BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics=atlas.createGraphics();
            graphics.setColor(Color.BLACK);graphics.fillRect(0,0,2048,1024);graphics.dispose();
            pack(atlas,back,1,1,frame);pack(atlas,front,12,1,frame);
            pack(atlas,edge,0,1,frame);pack(atlas,edge,11,1,frame);
            pack(atlas,end,1,0,frame);pack(atlas,end,11,0,frame);
            ImageIO.write(atlas,"png",output.resolve("lunar_raven_cape"+(frame==0?"":"_"+frame)+".png").toFile());
        }
    }
}
