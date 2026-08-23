package com.medgrid.strategy;

import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Ambulance;
import com.medgrid.model.Hospital;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Dijkstra;
import com.medgrid.protocol.Bid;

public class BestMatchStrategy implements BiddingStrategy {

    @Override
    public Bid generateAmbulanceBid(Ambulance ambulance, EmergencyCase eCase, Graph graph, String announcementId) {
        if (!ambulance.isAvailable()) {
            return new Bid(ambulance.getId(), announcementId, 0, false);
        }
        double eta = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), eCase.getLocation());
        // Simple inverse scoring for ETA
        double score = 1000.0 / (eta + 1);
        return new Bid(ambulance.getId(), announcementId, score, true);
    }

    @Override
    public Bid generateHospitalBid(Hospital hospital, EmergencyCase eCase, Graph graph, Ambulance ambulance, String announcementId) {
        if (hospital.getAvailableErBeds() == 0) {
            return new Bid(hospital.getId(), announcementId, 0, false);
        }
        double score = 100.0 - hospital.getLoadPercentage();
        if (hospital.hasSpecialty(eCase.getType())) {
            score += 50.0;
        }
        if (eCase.getSeverity().getPriority() >= 3 && hospital.getAvailableIcuBeds() > 0) {
            score += 50.0;
        }
        return new Bid(hospital.getId(), announcementId, score, true);
    }
}
