package com.medgrid.persistence;

import com.medgrid.model.Hospital;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataExporter {
    private static final DataExporter instance = new DataExporter();

    private final String timestamp;
    private final Path outputDir;
    private final Path logFile;
    private final Path resultsFile;
    private final ExecutorService writerService;

    private DataExporter() {
        timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        outputDir = Paths.get("data/output");
        logFile = outputDir.resolve("simulation_log_" + timestamp + ".txt");
        resultsFile = outputDir.resolve("results_summary_" + timestamp + ".csv");
        
        try {
            Files.createDirectories(outputDir);
            Files.writeString(resultsFile, "CaseID,Severity,Type,HospitalAssigned,AmbulanceID,ResponseTimeMs,Outcome\n", StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        writerService = Executors.newSingleThreadExecutor();
    }

    public static DataExporter getInstance() { return instance; }

    public void logEvent(String message) {
        String timedMessage = "[" + LocalDateTime.now() + "] " + message + "\n";
        writerService.submit(() -> {
            try {
                Files.writeString(logFile, timedMessage, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void recordCaseResult(String caseId, String severity, String type, String hospitalAssigned, String ambulanceId, long responseTime, String outcome) {
        String row = String.join(",", caseId, severity, type, hospitalAssigned, ambulanceId, String.valueOf(responseTime), outcome) + "\n";
        writerService.submit(() -> {
            try {
                Files.writeString(resultsFile, row, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void generateUtilizationReport(List<Hospital> hospitals) {
        Path reportFile = outputDir.resolve("hospital_utilization_report_" + timestamp + ".csv");
        StringBuilder sb = new StringBuilder("HospitalName,ERCapacity,ICUCapacity,CurrentLoad,Revenue\n");
        for (Hospital h : hospitals) {
            sb.append(String.join(",", 
                h.getId(), 
                String.valueOf(h.getAvailableErBeds()), 
                String.valueOf(h.getAvailableIcuBeds()), 
                String.format("%.1f", h.getLoadPercentage()), 
                "0.00" // To integrate with FinancialLedger, handled separately or here if passed
            )).append("\n");
        }
        try {
            Files.writeString(reportFile, sb.toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void shutdown() {
        writerService.shutdown();
    }
}
