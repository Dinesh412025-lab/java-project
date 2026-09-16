package com.medgrid.ai.agent;

import com.medgrid.ai.model.Bid;
import com.medgrid.ai.model.Hospital;
import com.medgrid.ai.model.PatientRequest;
import com.medgrid.ai.routing.DijkstraRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class HospitalAgent implements Agent {

    private final Hospital hospital;
    private final DijkstraRouter router;

    @Override
    public Bid processCallForProposal(PatientRequest request) {
        if (hospital.getAvailableIcuBeds() == 0 && request.getCondition().equalsIgnoreCase("Cardiac Arrest")) {
            return null; // Cannot bid
        }

        double conditionFit = calculateConditionFit(request.getCondition());
        
        String patientNode = router.findNearestNodeId(request.getLatitude(), request.getLongitude());
        String hospitalNode = router.findNearestNodeId(hospital.getLatitude(), hospital.getLongitude());
        double distance = router.calculateShortestPath(patientNode, hospitalNode);

        Bid bid = new Bid();
        bid.setAgentId(hospital.getId());
        bid.setConditionFitScore(conditionFit);
        bid.setEstimatedEta(distance); // In a real scenario, use time
        bid.setPatientRequestId(request.getId());
        
        // Combine into a simple score for now
        bid.setOverallScore(conditionFit * 100 - distance); 
        return bid;
    }

    private double calculateConditionFit(String condition) {
        // Tribuo ML Stub logic could be here
        if (hospital.getSpecializations().contains(condition.toUpperCase())) {
            return 0.9;
        }
        return 0.5;
    }

    @Override
    public void handleAcceptance(PatientRequest request) {
        if (request.getCondition().equalsIgnoreCase("Cardiac Arrest")) {
            hospital.setAvailableIcuBeds(hospital.getAvailableIcuBeds() - 1);
        } else {
            hospital.setAvailableWardBeds(hospital.getAvailableWardBeds() - 1);
        }
    }

    @Override
    public void handleRejection(PatientRequest request) {
        // No action needed
    }
    
    public Hospital getHospital() {
        return hospital;
    }
}
