package com.medgrid.config;

import com.medgrid.model.CaseType;

public class ScenarioRecord {
    public long timeOffsetMs;
    public String locationName;
    public String severity; // SEVERE, CRITICAL, MODERATE, MILD
    public CaseType type;

    public ScenarioRecord(long timeOffsetMs, String locationName, String severity, CaseType type) {
        this.timeOffsetMs = timeOffsetMs;
        this.locationName = locationName;
        this.severity = severity;
        this.type = type;
    }
}
