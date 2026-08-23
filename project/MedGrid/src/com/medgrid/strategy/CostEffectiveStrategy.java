package com.medgrid.strategy;

import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Ambulance;
import com.medgrid.model.Hospital;
import com.medgrid.routing.Graph;
import com.medgrid.routing.Dijkstra;
import com.medgrid.protocol.Bid;
import com.medgrid.finance.FinancialLedger;

public class CostEffectiveStrategy implements BiddingStrategy {

    @Override
    public Bid generateAmbulanceBid(Ambulance ambulance, EmergencyCase eCase, Graph graph, String announcementId) {
        if (!ambulance.isAvailable()) {
            return new Bid(ambulance.getId(), announcementId, Double.MAX_VALUE, false);
        }
        double distance = Dijkstra.computeShortestPathETA(graph, ambulance.getCurrentLocation(), eCase.getLocation());
        double estimatedCost = distance * 10.0;
        return new Bid(ambulance.getId(), announcementId, estimatedCost, true);
    }

    @Override
    public Bid generateHospitalBid(Hospital hospital, EmergencyCase eCase, Graph graph, Ambulance ambulance, String announcementId) {
        if (hospital.getAvailableErBeds() == 0 || !hospital.hasSpecialty(eCase.getType())) {
            return new Bid(hospital.getId(), announcementId, Double.MAX_VALUE, false);
        }
        // Bid based on lowest current revenue to balance the finances across hospitals
        double currentRevenue = FinancialLedger.getInstance().getRevenue(hospital.getId());
        return new Bid(hospital.getId(), announcementId, currentRevenue, true);
    }
}
