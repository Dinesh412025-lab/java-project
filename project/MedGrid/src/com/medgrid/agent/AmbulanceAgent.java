package com.medgrid.agent;

import com.medgrid.model.Ambulance;
import com.medgrid.model.EmergencyCase;
import com.medgrid.protocol.TaskAnnouncement;
import com.medgrid.protocol.Bid;
import com.medgrid.protocol.ContractNetManager;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Dijkstra;
import com.medgrid.strategy.BiddingStrategy;
import com.medgrid.finance.FinancialLedger;
import com.medgrid.monitoring.MonitoringService;
import com.medgrid.persistence.DataExporter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AmbulanceAgent extends Agent {
    private final Ambulance ambulance;
    private final BiddingStrategy strategy;
    private final Graph graph;
    private final List<HospitalAgent> hospitals;

    public AmbulanceAgent(Ambulance ambulance, BiddingStrategy strategy, Graph graph, List<HospitalAgent> hospitals) {
        super(ambulance.getId() + "-Agent");
        this.ambulance = ambulance;
        this.strategy = strategy;
        this.graph = graph;
        this.hospitals = hospitals;
        
        MonitoringService.getInstance().registerAmbulance(ambulance);
    }

    public Ambulance getAmbulance() {
        return ambulance;
    }

    public CompletableFuture<Bid> handleTaskAnnouncement(TaskAnnouncement announcement) {
        return CompletableFuture.supplyAsync(() -> {
            return strategy.generateAmbulanceBid(ambulance, announcement.getEmergencyCase(), graph, announcement.getAnnouncementId());
        }, executor);
    }

    public void assignTask(EmergencyCase eCase) {
        executor.submit(() -> {
            if (ambulance.setState(Ambulance.State.IDLE, Ambulance.State.EN_ROUTE_TO_PATIENT)) {
                MonitoringService.getInstance().logEvent("AMBULANCE_DISPATCH", ambulance.getId() + " dispatched to " + eCase.getCaseId());
                
                double distanceToPatient = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), eCase.getLocation());
                simulateTravel(distanceToPatient);
                
                ambulance.setCurrentLocation(eCase.getLocation());
                eCase.getPatientRecord().updateManagement("Picked up by " + ambulance.getId());
                eCase.getPatientRecord().setVitals("Stabilized in transit");
                FinancialLedger.getInstance().addCost(ambulance.getId(), distanceToPatient * 5.0); // Cost per distance

                // Negotiate with hospitals
                List<CompletableFuture<Bid>> hospitalBids = hospitals.stream()
                        .map(h -> h.receiveAmbulanceRequest("req-" + eCase.getCaseId(), eCase, ambulance))
                        .collect(Collectors.toList());

                Bid winningHospitalBid = ContractNetManager.evaluateBids(hospitalBids, true); // True if lower is better (FastestETA)

                if (winningHospitalBid != null) {
                    HospitalAgent winningHospital = hospitals.stream()
                            .filter(h -> h.getHospital().getId().equals(winningHospitalBid.getBidderId()))
                            .findFirst().orElse(null);

                    if (winningHospital != null) {
                        ambulance.setState(Ambulance.State.EN_ROUTE_TO_HOSPITAL);
                        MonitoringService.getInstance().logEvent("AMBULANCE_TRANSPORT", ambulance.getId() + " transporting to " + winningHospital.getHospital().getId());
                        
                        double distanceToHospital = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), winningHospital.getHospital().getLocation());
                        simulateTravel(distanceToHospital);
                        
                        ambulance.setCurrentLocation(winningHospital.getHospital().getLocation());
                        FinancialLedger.getInstance().addCost(ambulance.getId(), distanceToHospital * 5.0);
                        
                        winningHospital.admitPatient(eCase, ambulance, distanceToPatient + distanceToHospital);
                        
                        long totalTime = System.currentTimeMillis() - eCase.getCreationTime();
                        MonitoringService.getInstance().recordCaseResponse(totalTime);
                        DataExporter.getInstance().recordCaseResult(eCase.getCaseId(), eCase.getSeverity().name(), eCase.getType().name(), winningHospital.getHospital().getId(), ambulance.getId(), totalTime, "SUCCESS");
                    }
                } else {
                    MonitoringService.getInstance().logEvent("SYSTEM_ERROR", "No hospital available for case " + eCase.getCaseId());
                    DataExporter.getInstance().recordCaseResult(eCase.getCaseId(), eCase.getSeverity().name(), eCase.getType().name(), "NONE", ambulance.getId(), -1, "FAILED");
                }
                
                ambulance.setState(Ambulance.State.IDLE);
            }
        });
    }

    private void simulateTravel(double distance) {
        try {
            Thread.sleep((long)(distance * 50)); // Scale distance to sleep time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
