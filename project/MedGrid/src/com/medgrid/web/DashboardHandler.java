package com.medgrid.web;

import com.medgrid.model.Ambulance;
import com.medgrid.model.Hospital;
import com.medgrid.model.CaseType;
import com.medgrid.model.Medicine;
import com.medgrid.monitoring.MonitoringService;
import com.medgrid.finance.FinancialLedger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DashboardHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            String jsonResponse = generateJson();
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private String generateJson() {
        MonitoringService ms = MonitoringService.getInstance();
        FinancialLedger ledger = FinancialLedger.getInstance();

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // Hospitals
        sb.append("\"hospitals\": [");
        boolean firstH = true;
        for (Hospital h : ms.getHospitals()) {
            if (!firstH) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"id\": \"%s\",", h.getId()));
            sb.append(String.format("\"load\": %.1f,", h.getLoadPercentage()));
            sb.append(String.format("\"er_available\": %d,", h.getAvailableErBeds()));
            sb.append(String.format("\"icu_available\": %d,", h.getAvailableIcuBeds()));
            sb.append(String.format("\"revenue\": %.2f,", ledger.getRevenue(h.getId())));
            
            // Doctors
            sb.append("\"doctors\": {");
            boolean firstD = true;
            for (CaseType t : h.getDoctorAvailability().keySet()) {
                if (!firstD) sb.append(",");
                sb.append(String.format("\"%s\": %d", t.name(), h.getDoctorAvailability().get(t).get()));
                firstD = false;
            }
            sb.append("},");

            // Medicines
            sb.append("\"medicines\": {");
            boolean firstM = true;
            for (Medicine m : h.getMedicineInventory().keySet()) {
                if (!firstM) sb.append(",");
                sb.append(String.format("\"%s\": %d", m.name(), h.getMedicineInventory().get(m).get()));
                firstM = false;
            }
            sb.append("}");

            sb.append("}");
            firstH = false;
        }
        sb.append("],");

        // Ambulances
        sb.append("\"ambulances\": [");
        boolean firstA = true;
        for (Ambulance a : ms.getAmbulances()) {
            if (!firstA) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"id\": \"%s\",", a.getId()));
            sb.append(String.format("\"state\": \"%s\",", a.getState().name()));
            sb.append(String.format("\"location\": \"%s\",", a.getCurrentLocation().getId()));
            sb.append(String.format("\"cost\": %.2f", ledger.getCost(a.getId())));
            sb.append("}");
            firstA = false;
        }
        sb.append("],");

        // Metrics
        sb.append("\"metrics\": {");
        sb.append(String.format("\"totalCases\": %d,", ms.getTotalCases()));
        sb.append(String.format("\"avgResponseTime\": %.2f,", ms.getAverageResponseTime()));
        sb.append(String.format("\"totalRevenue\": %.2f,", ledger.getTotalRevenue()));
        sb.append(String.format("\"totalCost\": %.2f", ledger.getTotalCost()));
        sb.append("},");

        // Logs
        sb.append("\"logs\": [");
        boolean firstL = true;
        for (String log : ms.getRecentLogs()) {
            if (!firstL) sb.append(",");
            // simple escape
            sb.append(String.format("\"%s\"", log.replace("\"", "\\\"")));
            firstL = false;
        }
        sb.append("],");

        // History
        sb.append("\"history\": [");
        boolean firstHist = true;
        for (MonitoringService.DataPoint dp : ms.getHistory()) {
            if (!firstHist) sb.append(",");
            sb.append("{");
            sb.append(String.format("\"timestamp\": %d,", dp.timestamp));
            sb.append(String.format("\"cases\": %d,", dp.cases));
            sb.append(String.format("\"netRevenue\": %.2f", dp.netRevenue));
            sb.append("}");
            firstHist = false;
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }
}
