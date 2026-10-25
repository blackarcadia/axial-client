package com.axial.cosmetics.client;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerUpdateFeedTest {
    private static ServerUpdateFeed.Update parse(String items) throws Exception {
        return ServerUpdateFeed.parse(("<rss><channel>"+items+"</channel></rss>").getBytes(StandardCharsets.UTF_8));
    }
    private static String item(String title,String link,String date) {
        return "<item><title>"+title+"</title><link>"+link+"</link><pubDate>"+date+"</pubDate></item>";
    }
    @Test void readsTitleEntitiesLinkAndDateWithoutUsingPostHtml() throws Exception {
        var update=parse(item("<![CDATA[GEAR + PRESTIGE]]>","https://axialprisons.com/threads/gear.34/","Wed, 21 Oct 2026 18:12:02 +0000"));
        assertEquals("GEAR + PRESTIGE",update.title());
        assertEquals("https://axialprisons.com/threads/gear.34/",update.link().toString());
        assertEquals("2026-10-21",update.date().toString());
        assertEquals("Gear & enchants",parse(item("Gear &amp; enchants","/threads/gear.35/","")).title());
    }
    @Test void usesNewestPublicationRatherThanPinnedOrReplyBumpedOrder() throws Exception {
        var update=parse(item("Older pinned post","/threads/old.1/","Tue, 13 Oct 2026 14:43:56 +0000")
                +item("Latest update","/threads/new.2/","Wed, 21 Oct 2026 18:12:02 +0000")
                +item("Missing date","/threads/unknown.3/",""));
        assertEquals("Latest update",update.title());
    }
    @Test void onlyAcceptsDirectHttpsForumThreadLinks() throws Exception {
        for(String link:new String[]{"javascript:alert(1)","https://example.com/threads/1/",
                "https://axialprisons.com.evil.test/threads/1/","http://axialprisons.com/threads/1/",
                "https://axialprisons.com/store","https://user@axialprisons.com/threads/1/"})
            assertNull(parse(item("Bad link",link,"")),link);
    }
    @Test void emptyFeedIsValidButHtmlErrorPageAndExternalEntitiesAreRejected() throws Exception {
        assertNull(parse(""));
        assertThrows(Exception.class,()->ServerUpdateFeed.parse("<html><title>Unavailable</title></html>".getBytes(StandardCharsets.UTF_8)));
        assertThrows(Exception.class,()->ServerUpdateFeed.parse(("<!DOCTYPE rss [<!ENTITY secret SYSTEM 'file:///should-not-be-read'>]>"
                +"<rss><channel><item><title>&secret;</title></item></channel></rss>").getBytes(StandardCharsets.UTF_8)));
    }
    @Test void readsXenforoArticleImageFromEncodedHtml() throws Exception {
        String post=item("Update","/threads/update.34/","").replace("</item>",
                "<content:encoded><![CDATA[<div><img src='/attachments/lead-png.51/' width='1920'>"
                +"<img src='/attachments/second-png.52/'></div>]]></content:encoded></item>");
        assertEquals("https://axialprisons.com/attachments/lead-png.51/",parse(post).image().toString());
    }
    @Test void missingOrUnusableImageKeepsTheAnnouncement() throws Exception {
        assertNull(parse(item("Update","/threads/update.34/","")).image());
        String post=item("Update","/threads/update.34/","").replace("</item>",
                "<description><![CDATA[<img src='https://external.test/tracker.png'>"
                +"<img data-src='/data/cover.png?x=1&amp;y=2'>]]></description></item>");
        var update=parse(post);
        assertEquals("Update",update.title());
        assertEquals("https://axialprisons.com/data/cover.png?x=1&y=2",update.image().toString());
    }
    @Test void imageLinksRequirePublicForumImagePaths() {
        for(String source:new String[]{"file:///tmp/image.png","http://axialprisons.com/attachments/1/",
                "https://elsewhere.test/attachments/1/","https://axialprisons.com/store",
                "https://user@axialprisons.com/data/cover.png"})assertNull(ServerUpdateFeed.imageUri(source));
    }
}
