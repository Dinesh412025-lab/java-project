package com.medgrid.ai.agent;

import com.medgrid.ai.model.Bid;
import com.medgrid.ai.model.PatientRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatcherAgent {

    private final List<HospitalAgent> hospitalAgents = new ArrayList<>();
    private final List<AmbulanceAgent> ambulanceAgents = new ArrayList<>();

    public void registerHospitalAgent(HospitalAgent agent) {
        hospitalAgents.add(agent);
    }

    public void registerAmbulanceAgent(AmbulanceAgent agent) {
        ambulanceAgents.add(agent);
    }

    public DispatchResult dispatch(PatientRequest request) {
        // CFP to Hospitals
        List<Bid> hospitalBeds = hospitalAgents.stream()
                .map(agent -> agent.processCallForProposal(request))
                .filter(bid -> bid != null)
                .sorted(Comparator.comparingDouble(Bid::getOverallScore).reversed())
                .collect(Collectors.toList());

        // CFP to Ambulances
        List<Bid> ambulanceBids = ambulanceAgents.stream()
                .map(agent -> agent.processCallForProposal(request))
                .filter(bid -> bid != null)
                .sorted(Comparator.comparingDouble(Bid::getOverallScore).reversed())
                .collect(Collectors.toList());

        if (hospitalBeds.isEmpty() || ambulanceBids.isEmpty()) {
            return new DispatchResult(null, null, "No available resources.");
        }

        Bid winningHospitalBid = hospitalBeds.get(0);
        Bid winningAmbulanceBid = ambulanceBids.get(0);

        // Notify winners
        hospitalAgents.stream()
                .filter(a -> a.getHospital().getId().equals(winningHospitalBid.getAgentId()))
                .findFirst().ifPresent(a -> a.handleAcceptance(request));

        ambulanceAgents.stream()
                .filter(a -> a.getAmbulance().getId().equals(winningAmbulanceBid.getAgentId()))
                .findFirst().ifPresent(a -> a.handleAcceptance(request));

        return new DispatchResult(winningHospitalBid, winningAmbulanceBid, "Dispatched Successfully");
    }

    public static class DispatchResult {
        public Bid hospitalBid;
        public Bid ambulanceBid;
        public String status;

        public DispatchResult(Bid hBid, Bid aBid, String status) {
            this.hospitalBid = hBid;
            this.ambulanceBid = aBid;
            this.status = status;
        }
    }
}
