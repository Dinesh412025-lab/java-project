package com.medgrid.web;

import com.medgrid.analytics.HistoricalAnalytics;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class HistoryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HistoricalAnalytics.Summary summary = HistoricalAnalytics.getLatestSummary();
        
        String jsonResponse = String.format("{\"totalCases\": %d, \"avgResponseTimeMs\": %.2f, \"successRate\": %.1f}",
                summary.totalCases,
                summary.avgResponseTimeMs,
                summary.successRate);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, jsonResponse.length());

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes());
        }
    }
}
