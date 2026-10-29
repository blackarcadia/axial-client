import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Packs six cape faces into vanilla UVs and bakes a four-second shimmer.
 * Run from the project root: java -Djava.awt.headless=true tools/GenerateCreatorCape.java [admin|staff]
 */
class GenerateCreatorCape {
    private static final int SCALE = 32;
    private static final int WIDTH = 10 * SCALE;
    private static final int HEIGHT = 16 * SCALE;
    private static Path SOURCES = Path.of("assets/creator");

    private static boolean admin,staff;

    private static BufferedImage resize(String name, int width, int height) throws Exception {
        BufferedImage image = ImageIO.read(SOURCES.resolve(name).toFile());
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    private static BufferedImage ownerEdge() {
        BufferedImage edge = new BufferedImage(SCALE, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = edge.createGraphics();
        graphics.setColor(staff?new Color(7,10,7):admin?new Color(10,7,9):new Color(10, 9, 7));
        graphics.fillRect(0, 0, SCALE, HEIGHT);
        graphics.setColor(staff?new Color(47,99,12):admin?new Color(99,12,15):new Color(99, 65, 12));
        graphics.fillRect(1, 0, 1, HEIGHT);
        graphics.fillRect(SCALE - 2, 0, 1, HEIGHT);
        // Explicit block glyphs avoid machine-dependent fonts. Five tall letters fill
        // 484 of 512 pixels, with a 14-pixel margin at each end.
        String[][] letters = {
                {"01110", "11011", "11011", "11011", "11011", "11011", "01110"},
                {"10001", "10001", "10001", "10101", "10101", "11011", "10001"},
                {"10001", "11001", "11001", "10101", "10011", "10011", "10001"},
                {"11111", "10000", "10000", "11110", "10000", "10000", "11111"},
                {"11110", "10001", "10001", "11110", "10100", "10010", "10001"}
        };
        if(admin) letters=new String[][] {
                {"01110","11011","11011","11111","11011","11011","11011"},
                {"11110","10011","10001","10001","10001","10011","11110"},
                {"10001","11011","11111","10101","10001","10001","10001"},
                {"11111","00100","00100","00100","00100","00100","11111"},
                {"10001","11001","11001","10101","10011","10011","10001"}
        };
        if(staff) letters=new String[][] {
                {"01111","11000","11000","01110","00011","00011","11110"},
                {"11111","00100","00100","00100","00100","00100","00100"},
                {"01110","11011","11011","11111","11011","11011","11011"},
                {"11111","10000","10000","11110","10000","10000","10000"},
                {"11111","10000","10000","11110","10000","10000","10000"}
        };
        for (int letter = 0; letter < 5; letter++) {
            int top = 14 + letter * 100;
            for (int row = 0; row < 7; row++) for (int col = 0; col < 5; col++) {
                if (letters[letter][row].charAt(col) != '1') continue;
                graphics.setColor(staff?new Color(35,80,5):admin?new Color(95,5,8):new Color(95, 57, 7));
                graphics.fillRect(7 + col * 4, top + row * 12 + 2, 4, 12);
                for (int y = 0; y < 12; y++) {
                    double shine = (Math.cos((top + row * 12 + y) * 0.025) + 1) / 2;
                    graphics.setColor(staff?new Color(140+(int)(45*shine),199+(int)(56*shine),20+(int)(35*shine)):admin?new Color(199+(int)(56*shine),20+(int)(45*shine),28+(int)(32*shine)):new Color(199 + (int) (56 * shine), 139 + (int) (67 * shine), 27 + (int) (39 * shine)));
                    graphics.fillRect(6 + col * 4, top + row * 12 + y, 4, 1);
                }
            }
        }
        graphics.dispose();
        return edge;
    }

    private static BufferedImage cap(boolean bottom) {
        BufferedImage image = new BufferedImage(WIDTH, SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(staff?new Color(7,10,7):admin?new Color(10,7,9):new Color(10, 9, 7));
        graphics.fillRect(0, 0, WIDTH, SCALE);
        graphics.setColor(staff?new Color(48,108,12):admin?new Color(108,12,18):new Color(108, 71, 12));
        graphics.fillRect(0, 3, WIDTH, 2);
        graphics.fillRect(0, SCALE - 5, WIDTH, 2);
        for (int x = 0; x < WIDTH; x += 32) {
            graphics.setColor(staff?new Color(165,240,32):admin?new Color(224,30,42):new Color(224, 163, 32));
            int center = bottom ? SCALE - 13 : 8;
            graphics.fillRect(x + 12, center, 8, 8);
        }
        graphics.dispose();
        return image;
    }

    private static void pack(BufferedImage atlas, BufferedImage face, int u, int v, int frame) {
        for (int y = 0; y < face.getHeight(); y++) for (int x = 0; x < face.getWidth(); x++) {
            int rgb = face.getRGB(x, y);
            int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
            double phase = frame * Math.PI * 2 / 16 - x * 0.01125 - y * 0.00625;
            double highlight = Math.pow((Math.cos(phase) + 1) / 2, 10);
            // Each accent brightens in its own palette; charcoal remains untouched.
            boolean colored=staff?g>90&&g>r*1.15&&g>b*1.4:admin?r>90&&r>g*1.4&&r>b*1.4:r>90&&r>b*1.4&&g>b*1.2;
            double strength=colored?0.12*highlight:0;
            r += (int) (((staff?190:255) - r) * strength);
            g += (int) (((staff?255:admin?72:226) - g) * strength);
            b += (int) (((staff?70:admin?88:112) - b) * strength);
            atlas.setRGB(u * SCALE + x, v * SCALE + y, 0xFF000000 | (r << 16) | (g << 8) | b);
        }
    }

    public static void main(String[] args) throws Exception {
        admin=args.length>0&&args[0].equals("admin");
        staff=args.length>0&&args[0].equals("staff");
        SOURCES=Path.of(staff?"assets/staff":admin?"assets/admin":"assets/creator");
        BufferedImage front = resize("front.png", WIDTH, HEIGHT);
        BufferedImage back = resize("back.png", WIDTH, HEIGHT);
        BufferedImage edge = ownerEdge(), top = cap(false), bottom = cap(true);
        ImageIO.write(front, "png", SOURCES.resolve("cape-panel.png").toFile());
        ImageIO.write(edge, "png", SOURCES.resolve("left.png").toFile());
        ImageIO.write(edge, "png", SOURCES.resolve("right.png").toFile());
        ImageIO.write(top, "png", SOURCES.resolve("top.png").toFile());
        ImageIO.write(bottom, "png", SOURCES.resolve("bottom.png").toFile());
        Path output = Path.of("src/main/resources/assets/axial_cosmetics/textures/entity");
        Files.createDirectories(output);
        for (int frame = 0; frame < 16; frame++) {
            BufferedImage atlas = new BufferedImage(64 * SCALE, 32 * SCALE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = atlas.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, atlas.getWidth(), atlas.getHeight());
            graphics.dispose();
            // Cape model rotates its cuboid by 180 degrees: north is the outward back.
            pack(atlas, back, 1, 1, frame);
            pack(atlas, front, 12, 1, frame);
            pack(atlas, edge, 0, 1, frame);
            pack(atlas, edge, 11, 1, frame);
            pack(atlas, top, 1, 0, frame);
            pack(atlas, bottom, 11, 0, frame);
            String suffix = frame == 0 ? "" : "_" + frame;
            ImageIO.write(atlas, "png", output.resolve((staff?"staff_cape":admin?"admin_cape":"creator_cape") + suffix + ".png").toFile());
        }
    }
}
