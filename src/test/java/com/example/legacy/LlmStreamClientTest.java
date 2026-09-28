package com.example.legacy;

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

public class LlmStreamClientTest {
    @Test public void readsGemmaServerSentEventsAsOneJsonResult() throws Exception {
        final AtomicBoolean streamed = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", new HttpHandler() {
            @Override public void handle(HttpExchange exchange) throws IOException {
                byte[] request = new byte[8192];
                int count = exchange.getRequestBody().read(request);
                streamed.set(new String(request, 0, count, StandardCharsets.UTF_8).contains("\"stream\":true"));
                byte[] body = ("data: {\"choices\":[{\"delta\":{\"content\":\"{\\\"ok\\\":true}\"}}]}\n\n" +
                    "data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            }
        });
        server.start();
        String old = System.getProperty("llm.api.url");
        try {
            System.setProperty("llm.api.url", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            assertEquals("{\"ok\":true}", new LlmStreamClient().completeJson("system", "input"));
            assertTrue(streamed.get());
        } finally {
            if (old == null) System.clearProperty("llm.api.url"); else System.setProperty("llm.api.url", old);
            server.stop(0);
        }
    }
}