package com.medgrid.simulation;

import com.medgrid.model.*;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Node;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class EmergencyCaseFactory {
    private final Graph graph;
    private final Random random = new Random();

    public EmergencyCaseFactory(Graph graph) {
        this.graph = graph;
    }

    public EmergencyCase generateRandomCase() {
        List<Node> nodes = graph.getNodes().stream().collect(Collectors.toList());
        Location loc = nodes.get(random.nextInt(nodes.size())).getLocation();
        CaseSeverity severity = CaseSeverity.values()[random.nextInt(CaseSeverity.values().length)];
        CaseType type = CaseType.values()[random.nextInt(CaseType.values().length)];
        
        String patientId = "P-" + UUID.randomUUID().toString().substring(0, 5);
        PatientRecord record = new PatientRecord(patientId, "John Doe", 20 + random.nextInt(60), "O+");
        
        return new EmergencyCase("C-" + UUID.randomUUID().toString().substring(0, 5), loc, severity, type, record);
    }

    public EmergencyCase createCase(Location loc, CaseSeverity severity, CaseType type) {
        String patientId = "P-" + UUID.randomUUID().toString().substring(0, 5);
        PatientRecord record = new PatientRecord(patientId, "Jane Doe", 20 + random.nextInt(60), "A+");
        return new EmergencyCase("C-" + UUID.randomUUID().toString().substring(0, 5), loc, severity, type, record);
    }
}
