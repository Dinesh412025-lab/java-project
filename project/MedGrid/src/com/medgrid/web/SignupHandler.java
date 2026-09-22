package com.medgrid.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.PrintWriter;

public class SignupHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equals(exchange.getRequestMethod())) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            String body = sb.toString();
            String email = extractJsonField(body, "email");
            String password = extractJsonField(body, "password");

            if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing email or password\"}");
                return;
            }

            email = email.trim();
            password = password.trim();

            Path usersFile = Paths.get("data/input/users.csv");
            
            // Check if user already exists
            try (BufferedReader reader = new BufferedReader(new FileReader(usersFile.toFile()))) {
                String line;
                reader.readLine(); // skip header
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 1 && parts[0].trim().equalsIgnoreCase(email)) {
                        sendResponse(exchange, 409, "{\"error\": \"Account already exists\"}");
                        return;
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"Server configuration error\"}");
                return;
            }

            // Append new user
            try (PrintWriter out = new PrintWriter(new FileWriter(usersFile.toFile(), true))) {
                out.println(email + "," + password + ",USER");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to create account\"}");
                return;
            }

            sendResponse(exchange, 201, "{\"status\": \"success\", \"message\": \"Account created\"}");
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + key.length()) + 1;
        int end = json.indexOf("\"", start);
        if (start == 0 || end == -1) return null;
        return json.substring(start, end);
    }

    private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

