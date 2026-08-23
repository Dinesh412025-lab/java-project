package com.medgrid.protocol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ContractNetManager {

    public static Bid evaluateBids(List<CompletableFuture<Bid>> bidFutures, boolean lowerIsBetter) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(bidFutures.toArray(new CompletableFuture[0]));
        
        try {
            allOf.get(); // Wait for all bids
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        List<Bid> receivedBids = new ArrayList<>();
        for (CompletableFuture<Bid> future : bidFutures) {
            try {
                Bid bid = future.get();
                if (bid != null && bid.isCanHandle()) {
                    receivedBids.add(bid);
                }
            } catch (InterruptedException | ExecutionException e) {
                // Ignore failed bids
            }
        }

        if (receivedBids.isEmpty()) return null;

        Comparator<Bid> comparator = Comparator.comparingDouble(Bid::getBidValue);
        if (!lowerIsBetter) {
            comparator = comparator.reversed();
        }

        receivedBids.sort(comparator);
        return receivedBids.get(0);
    }
}
