package com.medgrid.finance;

import java.util.concurrent.atomic.AtomicInteger;

public class AncillaryService {
    private final String hospitalId;
    private final AtomicInteger labsPerformed = new AtomicInteger(0);
    private final AtomicInteger radiologyPerformed = new AtomicInteger(0);

    public AncillaryService(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public void performLabTest(String patientId) {
        labsPerformed.incrementAndGet();
        FinancialLedger.getInstance().addRevenue(hospitalId, 150.0);
    }

    public void performRadiology(String patientId) {
        radiologyPerformed.incrementAndGet();
        FinancialLedger.getInstance().addRevenue(hospitalId, 500.0);
    }
    
    public int getTotalServices() {
        return labsPerformed.get() + radiologyPerformed.get();
    }
}
