package com.axial.cosmetics.client;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Bounded, single-image cache. Download/decoding happen off-thread; GPU work only on render. */
final class ServerUpdateImage {
    private static final Identifier TEXTURE = Identifier.of("axial_cosmetics", "forum/latest_update");
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    record Pixels(int width, int height, int[] argb) {}
    record Thumbnail(Identifier texture, int width, int height) {}
    private static URI requested;
    private static Pixels pending;
    private static Thumbnail thumbnail;
    private static boolean loading;
    private static long nextAttempt, generation;

    static synchronized Thumbnail get(URI uri) {
        if (!java.util.Objects.equals(uri, requested)) {
            requested = uri; pending = null; loading = false; nextAttempt = 0; generation++;
            if (thumbnail != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE);
            thumbnail = null;
        }
        if (uri == null) return null;
        if (pending != null) {
            Pixels pixels = pending; pending = null;
            var image = new NativeImage(pixels.width(), pixels.height(), false);
            for (int y = 0; y < pixels.height(); y++)
                for (int x = 0; x < pixels.width(); x++) image.setColorArgb(x, y, pixels.argb()[y * pixels.width() + x]);
            var texture = new NativeImageBackedTexture(() -> "axial/forum_update", image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, texture);
            texture.upload();
            thumbnail = new Thumbnail(TEXTURE, pixels.width(), pixels.height());
        }
        long now = System.nanoTime();
        if (thumbnail == null && !loading && (nextAttempt == 0 || now - nextAttempt >= 0)) {
            loading = true;
            long job = generation;
            Thread.ofVirtual().name("axial-forum-image").start(() -> {
                Pixels pixels = null;
                try { pixels = download(uri); } catch (Exception ignored) { }
                synchronized (ServerUpdateImage.class) {
                    if (job == generation) {
                        pending = pixels; loading = false;
                        nextAttempt = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
                    }
                }
            });
        }
        return thumbnail;
    }

    private static Pixels download(URI uri) throws Exception {
        var connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(5_000); connection.setReadTimeout(10_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "AxialClient/ServerUpdates");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Forum image unavailable");
            try (var input = connection.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new IOException("Forum image too large");
                return decode(bytes);
            }
        } finally { connection.disconnect(); }
    }

    static Pixels decode(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported forum image");
            var reader = readers.next();
            BufferedImage source;
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16_777_216L)
                    throw new IOException("Forum image dimensions too large");
                var params = reader.getDefaultReadParam();
                int sample = Math.max(1, Math.max(width, height) / 512);
                params.setSourceSubsampling(sample, sample, 0, 0);
                source = reader.read(0, params);
            } finally { reader.dispose(); }
            try {
                double scale = Math.min(1, 512.0 / Math.max(source.getWidth(), source.getHeight()));
                int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
                var resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                try {
                    var graphics = resized.createGraphics();
                    try {
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(source, 0, 0, width, height, null);
                    } finally { graphics.dispose(); }
                    return new Pixels(width, height, resized.getRGB(0, 0, width, height, null, 0, width));
                } finally { resized.flush(); }
            } finally { source.flush(); }
        }
    }
}
