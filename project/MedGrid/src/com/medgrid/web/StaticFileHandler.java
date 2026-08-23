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
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }
}
