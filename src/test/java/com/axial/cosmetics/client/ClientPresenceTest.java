package com.axial.cosmetics.client;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientPresenceTest {
    @Test void mainMenuLabelUsesRequestedWordingAndNeverFakesZeroWhenUnavailable() {
        assertEquals("42 Space Rangers Online",ClientPresence.formatCount(42));
        assertEquals("0 Space Rangers Online",ClientPresence.formatCount(0));
        assertEquals("— Space Rangers Online",ClientPresence.formatCount(-1));
    }
    @Test void acceptsOnlyNonnegativeWholeCounts() throws Exception {
        assertEquals(0,ClientPresence.parseCount("{\"online\":0}"));
        assertEquals(42,ClientPresence.parseCount("{\"online\":42,\"ttlSeconds\":120}"));
        for(String invalid:new String[]{"{}","[]","null","bad","{\"online\":-1}","{\"online\":1.5}","{\"online\":true}","{\"online\":\"42\"}","{\"online\":99999999999999}"})
            assertThrows(IOException.class,()->ClientPresence.parseCount(invalid),invalid);
    }
    @Test void productionEndpointsRequireHttpsAndNoEmbeddedCredentials() {
        assertTrue(ClientPresence.validEndpoint(URI.create("https://example.com/online-count.php")));
        for(String bad:new String[]{"http://example.com/count","file:///secret","https://user:password@example.com/count","https://example.com/count#fragment","/relative"})
            assertFalse(ClientPresence.validEndpoint(URI.create(bad)),bad);
    }
    @Test void heartbeatSendsOnlyAnonymousSessionAndHandlesServiceFailures() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        String session=UUID.randomUUID().toString();
        AtomicInteger requests=new AtomicInteger();
        server.createContext("/heartbeat",exchange->{
            assertEquals("POST",exchange.getRequestMethod());
            assertEquals("application/json",exchange.getRequestHeaders().getFirst("Content-Type"));
            assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            var json=JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(1,json.size());assertEquals(session,json.get("sessionId").getAsString());
            requests.incrementAndGet();byte[] response="{\"online\":7}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });
        server.createContext("/failure",exchange->{exchange.sendResponseHeaders(503,-1);exchange.close();});
        server.createContext("/large",exchange->{byte[] response=new byte[4096];exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();});
        server.createContext("/redirect",exchange->{exchange.getResponseHeaders().add("Location","/heartbeat");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();
            assertEquals(7,ClientPresence.heartbeat(URI.create(base+"/heartbeat"),session));
            for(String path:new String[]{"/failure","/large","/redirect"})assertThrows(IOException.class,()->ClientPresence.heartbeat(URI.create(base+path),session));
            assertEquals(1,requests.get(),"Redirects must not trigger a second request");
        } finally {server.stop(0);}
    }
}
