import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Packs the supplied PNGs losslessly into vanilla cape UVs. No recolouring or animation. */
class GeneratePurrrCape {
    private static final Path SOURCE=Path.of("assets/purrr");
    private static BufferedImage read(String name,int width,int height) throws Exception {
        var image=ImageIO.read(SOURCE.resolve(name).toFile());
        if(image==null||image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException(name+" must be "+width+" × "+height);
        return image;
    }
    private static void pack(BufferedImage atlas,BufferedImage face,int u,int v){
        int[] pixels=face.getRGB(0,0,face.getWidth(),face.getHeight(),null,0,face.getWidth());
        atlas.setRGB(u*32,v*32,face.getWidth(),face.getHeight(),pixels,0,face.getWidth());
    }
    public static void main(String[] args) throws Exception {
        var back=read("back.png",320,512);var front=read("front.png",320,512);
        var side=read("side.png",32,512);var top=read("top.png",320,32);
        var atlas=new BufferedImage(2048,1024,BufferedImage.TYPE_INT_ARGB);
        // North is the outward face after the cape model's 180-degree rotation.
        pack(atlas,back,1,1);pack(atlas,front,12,1);
        pack(atlas,side,0,1);pack(atlas,side,11,1);
        pack(atlas,top,1,0);pack(atlas,top,11,0);
        var output=Path.of("src/main/resources/assets/axial_cosmetics/textures/entity/purrr_cape.png");
        Files.createDirectories(output.getParent());ImageIO.write(atlas,"png",output.toFile());
        // The admin catalogue displays the same outward-facing artwork as the wardrobe.
        ImageIO.write(back,"png",Path.of("backend/cosmetics/public/assets/purrr.png").toFile());
    }
}
