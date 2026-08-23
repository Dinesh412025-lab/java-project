package com.medgrid.strategy;

import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Ambulance;
import com.medgrid.model.Hospital;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Dijkstra;
import com.medgrid.protocol.Bid;

public class FastestETAStrategy implements BiddingStrategy {

    @Override
    public Bid generateAmbulanceBid(Ambulance ambulance, EmergencyCase eCase, Graph graph, String announcementId) {
        if (!ambulance.isAvailable()) {
            return new Bid(ambulance.getId(), announcementId, Double.MAX_VALUE, false);
        }
        double eta = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), eCase.getLocation());
        return new Bid(ambulance.getId(), announcementId, eta, true);
    }

    @Override
    public Bid generateHospitalBid(Hospital hospital, EmergencyCase eCase, Graph graph, Ambulance ambulance, String announcementId) {
        if (hospital.getAvailableErBeds() == 0 || !hospital.hasSpecialty(eCase.getType())) {
            return new Bid(hospital.getId(), announcementId, Double.MAX_VALUE, false);
        }
        double eta = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), hospital.getLocation());
        return new Bid(hospital.getId(), announcementId, eta, true);
    }
}
