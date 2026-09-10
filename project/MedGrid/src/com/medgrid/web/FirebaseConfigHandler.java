package com.medgrid.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Serves the public Firebase Web SDK configuration to the same-origin client. */
public class FirebaseConfigHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCommonHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String projectId = environment("FIREBASE_PROJECT_ID");
        String apiKey = environment("FIREBASE_WEB_API_KEY");
        String appId = environment("FIREBASE_APP_ID");
        String authDomain = environment("FIREBASE_AUTH_DOMAIN");
        if (authDomain.isEmpty() && !projectId.isEmpty()) {
            authDomain = projectId + ".firebaseapp.com";
        }

        if (projectId.isEmpty() || apiKey.isEmpty() || appId.isEmpty()) {
            sendJson(exchange, 503, "{\"error\":\"Firebase Authentication is not configured on the server.\"}");
            return;
        }

        String body = "{"
                + "\"apiKey\":\"" + jsonEscape(apiKey) + "\","
                + "\"authDomain\":\"" + jsonEscape(authDomain) + "\","
                + "\"projectId\":\"" + jsonEscape(projectId) + "\","
                + "\"appId\":\"" + jsonEscape(appId) + "\""
                + "}";
        sendJson(exchange, 200, body);
    }

    private String environment(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
