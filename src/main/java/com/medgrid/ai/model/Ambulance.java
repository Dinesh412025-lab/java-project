package com.medgrid.ai.model;

import lombok.Data;

@Data
public class Ambulance {
    private String id;
    private double latitude;
    private double longitude;
    
    public enum Status {
        FREE, EN_ROUTE, OCCUPIED
    }
    
    private Status status;
    private String type; // e.g., "ALS" (Advanced Life Support), "BLS"
}
