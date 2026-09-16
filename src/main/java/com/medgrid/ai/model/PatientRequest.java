package com.medgrid.ai.model;

import lombok.Data;

@Data
public class PatientRequest {
    private String id;
    private double latitude;
    private double longitude;
    private String condition;
}
