package com.medgrid.advisor;

import java.util.List;

public class Condition {
    private final String name;
    private final List<String> symptoms;
    private final String urgencyLevel;
    private final String otcGuidance;
    private final String specialistRecommended;

    public Condition(String name, List<String> symptoms, String urgencyLevel, String otcGuidance, String specialistRecommended) {
        this.name = name;
        this.symptoms = symptoms;
        this.urgencyLevel = urgencyLevel;
        this.otcGuidance = otcGuidance;
        this.specialistRecommended = specialistRecommended;
    }

    public String getName() {
        return name;
    }

    public List<String> getSymptoms() {
        return symptoms;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public String getOtcGuidance() {
        return otcGuidance;
    }

    public String getSpecialistRecommended() {
        return specialistRecommended;
    }
}
