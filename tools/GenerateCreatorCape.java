import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Packs the original logo into vanilla cape UVs and bakes a seamless shimmer.
 * Run from the project root: java tools/GenerateCreatorCape.java
 */
class GenerateCreatorCape {
    private static final int SCALE = 32;
    private static final int WIDTH = 10 * SCALE;
    private static final int HEIGHT = 16 * SCALE;

    public static void main(String[] args) throws Exception {
        BufferedImage logo = ImageIO.read(Path.of("assets/creator/original-logo.png").toFile());
        BufferedImage panel = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = panel.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            // The supplied image has white outer margins. Use the original black-backed
            // logo rectangle, preserving the artwork's aspect ratio and smooth details.
            int left = 64, top = 258, right = 1197, bottom = 927;
            int logoWidth = WIDTH - 16;
            int logoHeight = (int) Math.round(logoWidth * (bottom - top) / (double) (right - left));
            int logoY = (HEIGHT - logoHeight) / 2 - 25;
            graphics.drawImage(logo, 8, logoY, 8 + logoWidth, logoY + logoHeight,
                    left, top, right, bottom, null);
            graphics.setColor(new Color(77, 34, 109));
            graphics.fillRect(12, 18, 2, HEIGHT - 36);
            graphics.fillRect(WIDTH - 14, 18, 2, HEIGHT - 36);
            graphics.setColor(new Color(134, 69, 176));
            graphics.fillRect(28, HEIGHT - 24, WIDTH - 56, 2);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(panel, "png", Path.of("assets/creator/cape-panel.png").toFile());
        Path output = Path.of("src/main/resources/assets/axial_cosmetics/textures/entity");
        Files.createDirectories(output);
        for (int frame = 0; frame < 16; frame++) {
            BufferedImage atlas = new BufferedImage(64 * SCALE, 32 * SCALE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D background = atlas.createGraphics();
            background.setColor(Color.BLACK);
            background.fillRect(0, 0, atlas.getWidth(), atlas.getHeight());
            background.dispose();
            for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
                int rgb = panel.getRGB(x, y);
                int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
                // Only the violet artwork catches the moving light; fabric stays black.
                double phase = frame * Math.PI * 2 / 16 - x * 0.01125 - y * 0.00625;
                double highlight = Math.pow((Math.cos(phase) + 1) / 2, 10);
                double strength = b > 55 && b > g * 1.15 ? 0.16 * highlight : 0;
                r += (int) ((255 - r) * strength);
                g += (int) ((225 - g) * strength);
                b += (int) ((255 - b) * strength);
                int color = 0xFF000000 | (r << 16) | (g << 8) | b;
                atlas.setRGB(SCALE + x, SCALE + y, color);
                atlas.setRGB(12 * SCALE + x, SCALE + y, color);
                if (x < SCALE) atlas.setRGB(x, SCALE + y, color);
                if (x >= WIDTH - SCALE) atlas.setRGB(11 * SCALE + x - (WIDTH - SCALE), SCALE + y, color);
                if (y < SCALE) atlas.setRGB(SCALE + x, y, color);
                if (y >= HEIGHT - SCALE) atlas.setRGB(11 * SCALE + x, y - (HEIGHT - SCALE), color);
            }
            String suffix = frame == 0 ? "" : "_" + frame;
            ImageIO.write(atlas, "png", output.resolve("creator_cape" + suffix + ".png").toFile());
        }
    }
}
