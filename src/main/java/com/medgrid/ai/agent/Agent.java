package com.medgrid.ai.agent;

import com.medgrid.ai.model.Bid;
import com.medgrid.ai.model.PatientRequest;

public interface Agent {
    Bid processCallForProposal(PatientRequest request);
    void handleAcceptance(PatientRequest request);
    void handleRejection(PatientRequest request);
}
