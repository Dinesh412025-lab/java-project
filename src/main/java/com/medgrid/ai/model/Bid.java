package com.medgrid.ai.model;

import lombok.Data;

@Data
public class Bid {
    private String agentId; // Hospital or Ambulance ID
    private double conditionFitScore; // For hospitals
    private double estimatedEta; // For ambulances (or hospital distance)
    private double overallScore; // Combined metric
    private String patientRequestId;
}
