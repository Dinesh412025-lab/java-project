package com.medgrid.strategy;

import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Ambulance;
import com.medgrid.model.Hospital;
import com.medgrid.routing.Graph;
import com.medgrid.protocol.Bid;

public interface BiddingStrategy {
    Bid generateAmbulanceBid(Ambulance ambulance, EmergencyCase eCase, Graph graph, String announcementId);
    Bid generateHospitalBid(Hospital hospital, EmergencyCase eCase, Graph graph, Ambulance ambulance, String announcementId);
}
