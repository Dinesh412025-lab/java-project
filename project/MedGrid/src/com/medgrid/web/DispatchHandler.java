package com.medgrid.web;

import com.medgrid.agent.DispatchAgent;
import com.medgrid.model.CaseSeverity;
import com.medgrid.model.CaseType;
import com.medgrid.model.EmergencyCase;
import com.medgrid.model.PatientRecord;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Node;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DispatchHandler implements HttpHandler {
    
    private final DispatchAgent dispatchAgent;
    private final Graph graph;

    public DispatchHandler(DispatchAgent dispatchAgent, Graph graph) {
        this.dispatchAgent = dispatchAgent;
        this.graph = graph;
    }

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

            // Primitive JSON parsing for {"type": "DENTAL", "severity": "HIGH", "location": "North"}
            String body = sb.toString();
            String typeStr = extractJsonField(body, "type");
            String sevStr = extractJsonField(body, "severity");
            String locStr = extractJsonField(body, "location");
            String pwdStr = extractJsonField(body, "password");

            if (pwdStr == null || !pwdStr.equals("admin123")) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized: Invalid Admin Password\"}");
                return;
            }

            if (typeStr == null || sevStr == null || locStr == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing required fields\"}");
                return;
            }

            Node targetNode = null;
            for (Node n : graph.getNodes()) {
                if (n.getLocation().getId().equalsIgnoreCase(locStr)) {
                    targetNode = n;
                    break;
                }
            }

            if (targetNode == null) {
                sendResponse(exchange, 400, "{\"error\": \"Location not found in city graph\"}");
                return;
            }

            String caseId = "C-" + UUID.randomUUID().toString().substring(0, 5);
            PatientRecord record = new PatientRecord("P-" + System.currentTimeMillis(), "Unknown", 30, "O+");
            
            EmergencyCase eCase = new EmergencyCase(
                caseId,
                targetNode.getLocation(),
                CaseSeverity.valueOf(sevStr),
                CaseType.valueOf(typeStr),
                record
            );

            // Manually dispatch
            dispatchAgent.processEmergencyCall(eCase);

            sendResponse(exchange, 200, "{\"status\": \"dispatched\", \"caseId\": \"" + caseId + "\"}");
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
