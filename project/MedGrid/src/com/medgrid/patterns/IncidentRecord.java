package com.medgrid.patterns;

import com.medgrid.model.CaseSeverity;
import com.medgrid.model.CaseType;
import com.medgrid.model.Location;

import java.util.Objects;

/**
 * Spatiotemporal record of an emergency incident.
 */
public class IncidentRecord {
    private final String incidentId;
    private final Location location;
    private final double x;
    private final double y;
    private final long timestampMs;
    private final CaseSeverity severity;
    private final CaseType caseType;

    public IncidentRecord(String incidentId, Location location, long timestampMs, CaseSeverity severity, CaseType caseType) {
        this.incidentId = incidentId;
        this.location = location;
        this.x = location != null ? location.getX() : 0.0;
        this.y = location != null ? location.getY() : 0.0;
        this.timestampMs = timestampMs;
        this.severity = severity != null ? severity : CaseSeverity.MEDIUM;
        this.caseType = caseType != null ? caseType : CaseType.GENERAL;
    }

    public IncidentRecord(String incidentId, double x, double y, long timestampMs, CaseSeverity severity, CaseType caseType) {
        this.incidentId = incidentId;
        this.location = new Location("Loc-" + incidentId, (int) x, (int) y);
        this.x = x;
        this.y = y;
        this.timestampMs = timestampMs;
        this.severity = severity != null ? severity : CaseSeverity.MEDIUM;
        this.caseType = caseType != null ? caseType : CaseType.GENERAL;
    }

    public String getIncidentId() { return incidentId; }
    public Location getLocation() { return location; }
    public double getX() { return x; }
    public double getY() { return y; }
    public long getTimestampMs() { return timestampMs; }
    public CaseSeverity getSeverity() { return severity; }
    public CaseType getCaseType() { return caseType; }

    public double spatialDistance(IncidentRecord other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public long temporalDistance(IncidentRecord other) {
        return Math.abs(this.timestampMs - other.timestampMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncidentRecord that = (IncidentRecord) o;
        return Objects.equals(incidentId, that.incidentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId);
    }

    @Override
    public String toString() {
        return String.format("IncidentRecord{id='%s', loc=%s, t=%d, sev=%s, type=%s}",
                incidentId, location != null ? location.getId() : (x + "," + y), timestampMs, severity, caseType);
    }
}
