package com.medgrid.agent;

import com.medgrid.model.Hospital;
import com.medgrid.model.EmergencyCase;
import com.medgrid.model.Ambulance;
import com.medgrid.protocol.Bid;
import com.medgrid.routing.Graph;
import com.medgrid.strategy.BiddingStrategy;
import com.medgrid.operations.FacilityManager;
import com.medgrid.operations.StaffingSchedule;
import com.medgrid.finance.AncillaryService;
import com.medgrid.finance.BillingSystem;
import com.medgrid.finance.FinancialLedger;
import com.medgrid.monitoring.MonitoringService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HospitalAgent extends Agent {
    private final Hospital hospital;
    private final BiddingStrategy strategy;
    private final Graph graph;
    
    private final FacilityManager facilityManager;
    private final StaffingSchedule staffingSchedule;
    private final AncillaryService ancillaryService;

    public HospitalAgent(Hospital hospital, BiddingStrategy strategy, Graph graph) {
        super(hospital.getId() + "-Agent");
        this.hospital = hospital;
        this.strategy = strategy;
        this.graph = graph;
        
        this.facilityManager = new FacilityManager(hospital.getId(), 5000);
        this.staffingSchedule = new StaffingSchedule(hospital.getId(), 5, 15);
        this.ancillaryService = new AncillaryService(hospital.getId());
        
        MonitoringService.getInstance().registerHospital(hospital);
    }

    public CompletableFuture<Bid> receiveAmbulanceRequest(String announcementId, EmergencyCase eCase, Ambulance ambulance) {
        return CompletableFuture.supplyAsync(() -> {
            boolean hasMeds = eCase.getType().getRequiredMedicines().stream().allMatch(hospital::hasMedicine);
            if (!staffingSchedule.hasAvailableStaff() || facilityManager.getOxygenLevel() < 100 
                || !hospital.hasAvailableDoctor(eCase.getType()) || !hasMeds) {
                return new Bid(hospital.getId(), announcementId, Double.MAX_VALUE, false);
            }
            return strategy.generateHospitalBid(hospital, eCase, graph, ambulance, announcementId);
        }, executor);
    }

    public void admitPatient(EmergencyCase eCase, Ambulance ambulance, double distanceTraveled) {
        executor.submit(() -> {
            if (hospital.allocateErBed()) {
                eCase.getPatientRecord().updateManagement("Admitted to ER at " + hospital.getId());
                facilityManager.consumeOxygen(50);
                
                boolean useIcu = false;
                if (eCase.getSeverity().getPriority() >= 3 && hospital.allocateIcuBed()) {
                    eCase.getPatientRecord().updateManagement("Transferred to ICU at " + hospital.getId());
                    useIcu = true;
                    facilityManager.consumeOxygen(100);
                }

                // Consume medicines and allocate doctor
                hospital.allocateDoctor(eCase.getType());
                eCase.getType().getRequiredMedicines().forEach(hospital::deductMedicine);

                // Simulate some ancillary services
                ancillaryService.performLabTest(eCase.getPatientRecord().getPatientId());
                if (eCase.getSeverity().getPriority() >= 2) {
                    ancillaryService.performRadiology(eCase.getPatientRecord().getPatientId());
                }

                // Generate bill and record revenue
                double billAmount = BillingSystem.generateInvoice(eCase, distanceTraveled, useIcu, ancillaryService.getTotalServices());
                FinancialLedger.getInstance().addRevenue(hospital.getId(), billAmount);

                eCase.getPatientRecord().updateManagement("Patient stabilized. Billed: $" + String.format("%.2f", billAmount));
                MonitoringService.getInstance().logEvent("HOSPITAL_ADMIT", hospital.getId() + " admitted case " + eCase.getCaseId());
                
                final boolean finalUseIcu = useIcu;
                // Simulate patient stay, then release beds (simplified for simulation speed)
                executor.submit(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    hospital.releaseErBed();
                    hospital.releaseDoctor(eCase.getType());
                    if (finalUseIcu) hospital.releaseIcuBed();
                });
            } else {
                MonitoringService.getInstance().logEvent("HOSPITAL_ERROR", hospital.getId() + " failed to allocate ER bed for " + eCase.getCaseId());
            }
        });
    }

    public Hospital getHospital() {
        return hospital;
    }
}
