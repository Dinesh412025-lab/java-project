package com.medgrid.ai.model;

import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
public class Hospital {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    
    // Capacities
    private int totalIcuBeds;
    private int availableIcuBeds;
    private int totalWardBeds;
    private int availableWardBeds;
    
    // Resources & Specializations
    private Map<String, Integer> medicineStock; // e.g., "Aspirin": 100
    private List<String> specializations; // e.g., "CARDIOLOGY", "NEUROLOGY"
}
