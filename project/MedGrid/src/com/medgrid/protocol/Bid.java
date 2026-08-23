package com.medgrid.protocol;

public class Bid {
    private final String bidderId;
    private final String announcementId;
    private final double bidValue; // Could be ETA, match score, or cost
    private final boolean canHandle;

    public Bid(String bidderId, String announcementId, double bidValue, boolean canHandle) {
        this.bidderId = bidderId;
        this.announcementId = announcementId;
        this.bidValue = bidValue;
        this.canHandle = canHandle;
    }

    public String getBidderId() { return bidderId; }
    public String getAnnouncementId() { return announcementId; }
    public double getBidValue() { return bidValue; }
    public boolean isCanHandle() { return canHandle; }

    @Override
    public String toString() {
        return "Bid{id=" + bidderId + ", val=" + bidValue + ", can=" + canHandle + "}";
    }
}
