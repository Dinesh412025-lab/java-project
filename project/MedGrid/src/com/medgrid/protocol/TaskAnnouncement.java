package com.medgrid.protocol;

import com.medgrid.model.EmergencyCase;

public class TaskAnnouncement {
    private final String announcementId;
    private final EmergencyCase emergencyCase;

    public TaskAnnouncement(String announcementId, EmergencyCase emergencyCase) {
        this.announcementId = announcementId;
        this.emergencyCase = emergencyCase;
    }

    public String getAnnouncementId() { return announcementId; }
    public EmergencyCase getEmergencyCase() { return emergencyCase; }
}
