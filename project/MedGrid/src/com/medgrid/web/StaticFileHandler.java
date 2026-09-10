package com.medgrid.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private static final String WEB_DIR = new File("src/web").exists() ? "src/web" : "../src/web";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(WEB_DIR, path);
        if (file.exists() && file.isFile()) {
            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html";
            else if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".js")) contentType = "application/javascript";
            
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            if (path.equals("/index.html")) {
                String html = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
                String tag = "<" + "script";
                String endTag = "<" + "/script>";
                html = html.replace(tag + " src=\"app.js\"" + endTag, tag + " type=\"module\" src=\"firebase-auth.js\"" + endTag + "\n    " + tag + " src=\"app.js\"" + endTag);
                fileBytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }
}
