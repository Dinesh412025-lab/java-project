package com.medgrid.analytics;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class HistoricalAnalytics {

    public static class Summary {
        public long totalCases;
        public double avgResponseTimeMs;
        public double successRate;
    }

    private static Summary latestSummary = new Summary();

    public static Summary getLatestSummary() {
        return latestSummary;
    }

    public static void computeAndPrintGlobalMetrics() {
        System.out.println("=======================================================");
        System.out.println("            HISTORICAL ANALYTICS MODULE                ");
        System.out.println("=======================================================");
        
        Path outputDir = Paths.get("data/output");
        if (!Files.exists(outputDir)) {
            System.out.println("No historical data found.\n");
            return;
        }

        long[] totalCases = {0};
        long[] totalResponseTime = {0};
        int[] successfulOutcomes = {0};

        try (Stream<Path> paths = Files.list(outputDir)) {
            paths.filter(p -> p.getFileName().toString().startsWith("results_summary_") && p.toString().endsWith(".csv"))
                 .forEach(path -> {
                     try (BufferedReader br = Files.newBufferedReader(path)) {
                         String line;
                         boolean first = true;
                         while ((line = br.readLine()) != null) {
                             if (first) { first = false; continue; } // Skip header
                             if (line.trim().isEmpty()) continue;
                             
                             String[] parts = line.split(",");
                             if (parts.length >= 7) {
                                 totalCases[0]++;
                                 totalResponseTime[0] += Long.parseLong(parts[5]);
                                 if (parts[6].equalsIgnoreCase("SUCCESS")) {
                                     successfulOutcomes[0]++;
                                 }
                             }
                         }
                     } catch (Exception e) {
                         // ignore
                     }
                 });
        } catch (Exception e) {
            System.err.println("Error reading historical files: " + e.getMessage());
        }

        if (totalCases[0] == 0) {
            System.out.println("No historical cases recorded yet.\n");
        } else {
            latestSummary.totalCases = totalCases[0];
            latestSummary.avgResponseTimeMs = totalResponseTime[0] / (double)totalCases[0];
            latestSummary.successRate = (successfulOutcomes[0] / (double)totalCases[0]) * 100;
            
            System.out.println("Total Historical Cases Handled: " + latestSummary.totalCases);
            System.out.println("Global Average Response Time: " + latestSummary.avgResponseTimeMs + " ms");
            System.out.println("Global Success Rate: " + String.format("%.1f", latestSummary.successRate) + "%");
            System.out.println("=======================================================\n");
        }
    }
}
