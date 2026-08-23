package com.medgrid.model;

public class EmergencyCase implements Comparable<EmergencyCase> {
    private final String caseId;
    private final Location location;
    private final CaseSeverity severity;
    private final CaseType type;
    private final PatientRecord patientRecord;
    private long creationTime;

    public EmergencyCase(String caseId, Location location, CaseSeverity severity, CaseType type, PatientRecord patientRecord) {
        this.caseId = caseId;
        this.location = location;
        this.severity = severity;
        this.type = type;
        this.patientRecord = patientRecord;
        this.creationTime = System.currentTimeMillis();
    }

    public String getCaseId() { return caseId; }
    public Location getLocation() { return location; }
    public CaseSeverity getSeverity() { return severity; }
    public CaseType getType() { return type; }
    public PatientRecord getPatientRecord() { return patientRecord; }
    public long getCreationTime() { return creationTime; }

    @Override
    public String toString() {
        return "Case{" + caseId + ", " + type + ", " + severity + " at " + location + "}";
    }

    @Override
    public int compareTo(EmergencyCase other) {
        // Higher priority first
        int severityCompare = Integer.compare(other.getSeverity().getPriority(), this.getSeverity().getPriority());
        if (severityCompare != 0) {
            return severityCompare;
        }
        // If same severity, older creation time first
        return Long.compare(this.creationTime, other.creationTime);
    }
}
