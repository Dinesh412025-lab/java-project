package com.medgrid.finance;

import com.medgrid.model.EmergencyCase;
import com.medgrid.model.PatientRecord;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class BillingSystem {

    public static double generateInvoice(EmergencyCase eCase, double distanceTraveled, boolean icuUsed, int ancillaryCount) {
        double baseRate = 500.0;
        
        double severityMultiplier = eCase.getSeverity().getPriority() * 1.5;
        double distanceCost = distanceTraveled * 10.0; // $10 per unit
        
        double icuCost = icuUsed ? 2000.0 : 0.0;
        double ancillaryCost = ancillaryCount * 250.0;
        
        double totalCost = (baseRate * severityMultiplier) + distanceCost + icuCost + ancillaryCost;

        // Generate physical invoice
        try {
            Path invoiceDir = Paths.get("data/invoices");
            if (!Files.exists(invoiceDir)) {
                Files.createDirectories(invoiceDir);
            }
            Path invoiceFile = invoiceDir.resolve("Invoice_" + eCase.getCaseId() + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(invoiceFile.toFile()))) {
                writer.write("=======================================\n");
                writer.write("           MEDGRID INVOICE             \n");
                writer.write("=======================================\n");
                writer.write("Date: " + LocalDateTime.now() + "\n");
                writer.write("Case ID: " + eCase.getCaseId() + "\n");
                writer.write("Patient ID: " + eCase.getPatientRecord().getPatientId() + "\n");
                writer.write("Severity: " + eCase.getSeverity() + "\n");
                writer.write("---------------------------------------\n");
                writer.write("Base Cost (x Severity): $" + String.format("%.2f", (baseRate * severityMultiplier)) + "\n");
                writer.write("Ambulance Distance Cost: $" + String.format("%.2f", distanceCost) + "\n");
                writer.write("ICU Cost: $" + String.format("%.2f", icuCost) + "\n");
                writer.write("Ancillary Services Cost: $" + String.format("%.2f", ancillaryCost) + "\n");
                writer.write("---------------------------------------\n");
                writer.write("TOTAL DUE: $" + String.format("%.2f", totalCost) + "\n");
                writer.write("=======================================\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to generate invoice file: " + e.getMessage());
        }

        return totalCost;
    }
}
