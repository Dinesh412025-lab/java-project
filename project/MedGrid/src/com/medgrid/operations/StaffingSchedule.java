package com.medgrid.operations;

import java.util.concurrent.atomic.AtomicInteger;

public class StaffingSchedule {
    private final String hospitalId;
    private final AtomicInteger doctorsOnShift;
    private final AtomicInteger nursesOnShift;

    public StaffingSchedule(String hospitalId, int initialDoctors, int initialNurses) {
        this.hospitalId = hospitalId;
        this.doctorsOnShift = new AtomicInteger(initialDoctors);
        this.nursesOnShift = new AtomicInteger(initialNurses);
    }

    public int getDoctorsOnShift() {
        return doctorsOnShift.get();
    }

    public int getNursesOnShift() {
        return nursesOnShift.get();
    }

    public void changeShift(int newDoctors, int newNurses) {
        doctorsOnShift.set(newDoctors);
        nursesOnShift.set(newNurses);
    }

    public boolean hasAvailableStaff() {
        return doctorsOnShift.get() > 0 && nursesOnShift.get() > 0;
    }
}
