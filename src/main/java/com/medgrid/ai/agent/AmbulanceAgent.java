package com.medgrid.ai.agent;

import com.medgrid.ai.model.Ambulance;
import com.medgrid.ai.model.Bid;
import com.medgrid.ai.model.PatientRequest;
import com.medgrid.ai.routing.DijkstraRouter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AmbulanceAgent implements Agent {
    
    private final Ambulance ambulance;
    private final DijkstraRouter router;

    @Override
    public Bid processCallForProposal(PatientRequest request) {
        if (ambulance.getStatus() != Ambulance.Status.FREE) {
            return null;
        }

        String patientNode = router.findNearestNodeId(request.getLatitude(), request.getLongitude());
        String ambNode = router.findNearestNodeId(ambulance.getLatitude(), ambulance.getLongitude());
        double eta = router.calculateShortestPath(ambNode, patientNode);

        Bid bid = new Bid();
        bid.setAgentId(ambulance.getId());
        bid.setEstimatedEta(eta);
        bid.setOverallScore(-eta); // Lower ETA is better
        bid.setPatientRequestId(request.getId());
        return bid;
    }

    @Override
    public void handleAcceptance(PatientRequest request) {
        ambulance.setStatus(Ambulance.Status.EN_ROUTE);
    }

    @Override
    public void handleRejection(PatientRequest request) {
    }
    
    public Ambulance getAmbulance() {
        return ambulance;
    }
}
