import com.axial.cosmetics.client.GoldSparkGeometry;
import com.axial.cosmetics.client.HexwindGeometry.Point;
import com.axial.cosmetics.client.HexwindGeometry.Quad;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;

/** Generates previews from the exact live aura mesh; no proprietary reference assets are used. */
public class GenerateGoldSparkPreview {
    static Point p(float x,float y,float z){return new Point(x,y,z);}
    static void box(List<Quad> mesh,float x,float y,float z,float w,float h,float d,int color) {
        Point a=p(x,y,z),b=p(x+w,y,z),c=p(x+w,y+h,z),e=p(x,y+h,z);
        Point aa=p(x,y,z+d),bb=p(x+w,y,z+d),cc=p(x+w,y+h,z+d),ee=p(x,y+h,z+d);
        mesh.add(new Quad(a,b,c,e,color));mesh.add(new Quad(aa,ee,cc,bb,color));
        mesh.add(new Quad(a,e,ee,aa,color));mesh.add(new Quad(b,bb,cc,c,color));
        mesh.add(new Quad(e,c,cc,ee,color));mesh.add(new Quad(a,aa,bb,b,color));
    }
    static double depth(Point p){return -p.x()*.57+p.z()*.82;}
    static double sx(Point p){return p.x()*.82+p.z()*.57;}
    static double sy(Point p){return p.y()+depth(p)*.17;}
    static BufferedImage image(int size,float ticks) {
        var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        var mesh=new ArrayList<Quad>();
        // A neutral, block-shaped preview mannequin gives scale without borrowing a player skin.
        box(mesh,-.22f,1.48f,-.22f,.44f,.44f,.44f,0xff8b8098);
        box(mesh,-.24f,.74f,-.13f,.48f,.74f,.26f,0xff52425e);
        box(mesh,-.47f,.78f,-.13f,.21f,.68f,.26f,0xff6b5a77);
        box(mesh,.26f,.78f,-.13f,.21f,.68f,.26f,0xff6b5a77);
        box(mesh,-.22f,0,-.12f,.2f,.74f,.24f,0xff3e3448);
        box(mesh,.02f,0,-.12f,.2f,.74f,.24f,0xff3e3448);
        mesh.addAll(GoldSparkGeometry.frame(ticks,true));
        mesh.sort(Comparator.comparingDouble((Quad q)->(depth(q.a())+depth(q.b())+depth(q.c())+depth(q.d()))/4).reversed());
        double scale=size*.30;
        for(Quad q:mesh) {
            Polygon polygon=new Polygon();for(Point v:List.of(q.a(),q.b(),q.c(),q.d()))polygon.addPoint((int)Math.round(size*.5+sx(v)*scale),(int)Math.round(size*.82-sy(v)*scale));
            g.setColor(new Color(q.color(),true));g.fillPolygon(polygon);
        }
        g.dispose();return image;
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args.length==0?".":args[0]);
        Path gui=root.resolve("src/main/resources/assets/axial_cosmetics/textures/gui");Files.createDirectories(gui);
        var atlas=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);var ag=atlas.createGraphics();
        for(int i=0;i<64;i++)ag.drawImage(image(128,i*GoldSparkGeometry.PERIOD/64),(i%8)*128,(i/8)*128,null);ag.dispose();
        ImageIO.write(atlas,"png",gui.resolve("gold_spark_preview.png").toFile());
        Path assets=root.resolve("backend/cosmetics/public/assets");Files.createDirectories(assets);
        ImageIO.write(image(384,86),"png",assets.resolve("gold_spark.png").toFile());
        var writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(var output=new FileImageOutputStream(assets.resolve("gold_spark.gif").toFile())) {
            writer.setOutput(output);writer.prepareWriteSequence(null);
            for(int i=0;i<64;i++) {
                // GIF has one-bit alpha. Flatten translucent smoke onto the admin's dark backdrop.
                var frame=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);var fg=frame.createGraphics();
                fg.setColor(new Color(0x18151f));fg.fillRect(0,0,256,256);fg.drawImage(image(256,i*GoldSparkGeometry.PERIOD/64),0,0,null);fg.dispose();
                var metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frame),null);
                var tree=(IIOMetadataNode)metadata.getAsTree(metadata.getNativeMetadataFormatName());
                var control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("transparentColorIndex","0");control.setAttribute("delayTime",i%8<3?"10":"9");
                if(i==0) {
                    var extensions=new IIOMetadataNode("ApplicationExtensions");var loop=new IIOMetadataNode("ApplicationExtension");loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});extensions.appendChild(loop);tree.appendChild(extensions);
                }
                metadata.setFromTree(metadata.getNativeMetadataFormatName(),tree);writer.writeToSequence(new IIOImage(frame,null,metadata),null);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
        System.out.println("Generated Gold Spark preview atlas, looping admin GIF, still preview and utility texture.");
    }
}
