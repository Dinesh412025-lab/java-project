package com.medgrid.model;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Hospital {
    private final String id;
    private final Location location;
    
    private final int totalErBeds;
    private final AtomicInteger availableErBeds;
    
    private final int totalIcuBeds;
    private final AtomicInteger availableIcuBeds;
    
    private final Map<CaseType, AtomicInteger> doctorAvailability;
    private final Map<Medicine, AtomicInteger> medicineInventory;

    public Hospital(String id, Location location, int totalErBeds, int totalIcuBeds, Set<CaseType> specialties) {
        this.id = id;
        this.location = location;
        this.totalErBeds = totalErBeds;
        this.availableErBeds = new AtomicInteger(totalErBeds);
        this.totalIcuBeds = totalIcuBeds;
        this.availableIcuBeds = new AtomicInteger(totalIcuBeds);
        
        this.doctorAvailability = new ConcurrentHashMap<>();
        for (CaseType spec : specialties) {
            // Seed 2 to 5 doctors per specialty
            int count = 2 + (int)(Math.random() * 4);
            this.doctorAvailability.put(spec, new AtomicInteger(count));
        }

        this.medicineInventory = new ConcurrentHashMap<>();
        for (Medicine med : Medicine.values()) {
            // Seed 50 to 200 units per medicine
            int count = 50 + (int)(Math.random() * 150);
            this.medicineInventory.put(med, new AtomicInteger(count));
        }
    }

    public String getId() { return id; }
    public Location getLocation() { return location; }
    
    public int getAvailableErBeds() { return availableErBeds.get(); }
    public boolean allocateErBed() {
        while (true) {
            int current = availableErBeds.get();
            if (current == 0) return false;
            if (availableErBeds.compareAndSet(current, current - 1)) return true;
        }
    }
    public void releaseErBed() {
        availableErBeds.incrementAndGet();
    }

    public int getAvailableIcuBeds() { return availableIcuBeds.get(); }
    public boolean allocateIcuBed() {
        while (true) {
            int current = availableIcuBeds.get();
            if (current == 0) return false;
            if (availableIcuBeds.compareAndSet(current, current - 1)) return true;
        }
    }
    public void releaseIcuBed() {
        availableIcuBeds.incrementAndGet();
    }
    
    public double getLoadPercentage() {
        int totalAvailable = availableErBeds.get() + availableIcuBeds.get();
        int totalCapacity = totalErBeds + totalIcuBeds;
        return ((double)(totalCapacity - totalAvailable) / totalCapacity) * 100.0;
    }

    public boolean hasSpecialty(CaseType type) {
        return doctorAvailability.containsKey(type);
    }

    public boolean hasAvailableDoctor(CaseType type) {
        AtomicInteger count = doctorAvailability.get(type);
        return count != null && count.get() > 0;
    }

    public boolean allocateDoctor(CaseType type) {
        AtomicInteger count = doctorAvailability.get(type);
        if (count == null) return false;
        while (true) {
            int current = count.get();
            if (current == 0) return false;
            if (count.compareAndSet(current, current - 1)) return true;
        }
    }

    public void releaseDoctor(CaseType type) {
        AtomicInteger count = doctorAvailability.get(type);
        if (count != null) {
            count.incrementAndGet();
        }
    }

    public boolean hasMedicine(Medicine med) {
        AtomicInteger count = medicineInventory.get(med);
        return count != null && count.get() > 0;
    }

    public boolean deductMedicine(Medicine med) {
        AtomicInteger count = medicineInventory.get(med);
        if (count == null) return false;
        while (true) {
            int current = count.get();
            if (current == 0) return false;
            if (count.compareAndSet(current, current - 1)) return true;
        }
    }

    public Map<CaseType, AtomicInteger> getDoctorAvailability() {
        return doctorAvailability;
    }

    public Map<Medicine, AtomicInteger> getMedicineInventory() {
        return medicineInventory;
    }

    @Override
    public String toString() {
        return "Hospital{" + id + ", load=" + String.format("%.1f", getLoadPercentage()) + "%}";
    }
}
