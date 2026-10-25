package com.axial.cosmetics.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** One shared, asynchronously refreshed copy of the public Server Updates feed. */
final class ServerUpdateFeed {
    static final URI FORUM = URI.create("https://axialprisons.com/forums/server-updates.11/");
    private static final URI FEED = FORUM.resolve("index.rss");
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    record Update(String title, URI link, LocalDate date) {}
    record State(Update update, String message) {}
    private static volatile State state = new State(null, "Loading latest update…");
    private static long nextCheck;
    private static boolean fetching;

    static State current() { refresh(); return state; }

    private static synchronized void refresh() {
        long now = System.nanoTime();
        if (fetching || (nextCheck != 0 && now - nextCheck < 0)) return;
        fetching = true;
        Thread.ofVirtual().name("axial-forum-updates").start(() -> {
            boolean success = false;
            try {
                Update update = fetch();
                state = new State(update, update == null ? "No updates posted yet" : "");
                success = true;
            } catch (Exception ignored) {
                // Keep the last successful announcement if the website temporarily goes offline.
                if (state.update() == null) state = new State(null, "Updates unavailable");
            } finally {
                synchronized (ServerUpdateFeed.class) {
                    nextCheck = System.nanoTime() + TimeUnit.SECONDS.toNanos(success ? 300 : 60);
                    fetching = false;
                }
            }
        });
    }

    private static Update fetch() throws Exception {
        var connection = (HttpURLConnection) FEED.toURL().openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("User-Agent", "AxialClient/ServerUpdates");
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Forum feed unavailable");
            try (var input = connection.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new IOException("Forum feed too large");
                return parse(bytes);
            }
        } finally { connection.disconnect(); }
    }

    static Update parse(byte[] xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        if (!"rss".equals(document.getDocumentElement().getTagName())
                || document.getElementsByTagName("channel").getLength() == 0)
            throw new IOException("Not a forum RSS feed");
        var items = document.getElementsByTagName("item");
        Update newest = null;
        java.time.Instant newestTime = java.time.Instant.MIN;
        for (int i = 0; i < items.getLength(); i++) {
            var item = (Element) items.item(i);
            String title = text(item, "title").replaceAll("[\\p{Cntrl}\\s]+", " ").strip();
            if (title.isBlank()) continue;
            URI link;
            try { link = FORUM.resolve(text(item, "link")); }
            catch (IllegalArgumentException ignored) { continue; }
            if (!"https".equalsIgnoreCase(link.getScheme()) || !"axialprisons.com".equalsIgnoreCase(link.getHost())
                    || link.getUserInfo() != null || (link.getPort() != -1 && link.getPort() != 443)
                    || !link.getPath().startsWith("/threads/")) continue;
            ZonedDateTime published = null;
            try { published = ZonedDateTime.parse(text(item, "pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME); }
            catch (java.time.format.DateTimeParseException ignored) { }
            var time = published == null ? java.time.Instant.MIN : published.toInstant();
            if (newest == null || time.isAfter(newestTime)) {
                newest = new Update(title, link, published == null ? null : published.toLocalDate());
                newestTime = time;
            }
        }
        return newest;
    }

    private static String text(Element item, String tag) {
        var elements = item.getElementsByTagName(tag);
        return elements.getLength() == 0 ? "" : elements.item(0).getTextContent().strip();
    }
}
