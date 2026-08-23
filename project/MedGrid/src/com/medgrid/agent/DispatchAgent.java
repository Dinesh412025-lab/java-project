package com.medgrid.agent;

import com.medgrid.model.EmergencyCase;
import com.medgrid.protocol.TaskAnnouncement;
import com.medgrid.protocol.Bid;
import com.medgrid.protocol.ContractNetManager;
import com.medgrid.monitoring.MonitoringService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

public class DispatchAgent extends Agent {
    private final List<AmbulanceAgent> ambulances;
    private final boolean preferLowerBids; // True for ETA, False for Match Score
    private final PriorityBlockingQueue<EmergencyCase> pendingQueue;
    private volatile boolean isRunning = true;

    public DispatchAgent(List<AmbulanceAgent> ambulances, boolean preferLowerBids) {
        super("DispatchCenter");
        this.ambulances = ambulances;
        this.preferLowerBids = preferLowerBids;
        this.pendingQueue = new PriorityBlockingQueue<>();
        
        // Background thread to process the queue
        executor.submit(() -> {
            while (isRunning) {
                try {
                    EmergencyCase eCase = pendingQueue.take();
                    boolean dispatched = attemptDispatch(eCase);
                    if (!dispatched) {
                        // Re-queue and sleep a bit if all ambulances are busy
                        pendingQueue.put(eCase);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void processEmergencyCall(EmergencyCase eCase) {
        MonitoringService.getInstance().logEvent("DISPATCH_CALL", "Received call for " + eCase.getType() + " at " + eCase.getLocation() + " [Priority Added to Queue]");
        pendingQueue.put(eCase);
    }

    private boolean attemptDispatch(EmergencyCase eCase) {
        String announcementId = UUID.randomUUID().toString();
        TaskAnnouncement announcement = new TaskAnnouncement(announcementId, eCase);

        List<CompletableFuture<Bid>> bids = ambulances.stream()
                .map(a -> a.handleTaskAnnouncement(announcement))
                .collect(Collectors.toList());

        Bid winningBid = ContractNetManager.evaluateBids(bids, preferLowerBids);

        if (winningBid != null) {
            AmbulanceAgent winner = ambulances.stream()
                    .filter(a -> a.getAmbulance().getId().equals(winningBid.getBidderId()))
                    .findFirst().orElse(null);

            if (winner != null) {
                MonitoringService.getInstance().logEvent("DISPATCH_ASSIGN", winner.getAmbulance().getId() + " won bid for case " + eCase.getCaseId() + " with score: " + String.format("%.2f", winningBid.getBidValue()));
                winner.assignTask(eCase);
                return true;
            }
        }
        return false;
    }
}
