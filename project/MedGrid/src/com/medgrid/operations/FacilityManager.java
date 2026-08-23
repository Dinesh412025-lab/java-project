package com.medgrid.operations;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacilityManager {
    private final String hospitalId;
    private final AtomicInteger oxygenSupplyLitres;
    private final AtomicBoolean generatorActive;

    public FacilityManager(String hospitalId, int initialOxygen) {
        this.hospitalId = hospitalId;
        this.oxygenSupplyLitres = new AtomicInteger(initialOxygen);
        this.generatorActive = new AtomicBoolean(false);
    }

    public void consumeOxygen(int amount) {
        oxygenSupplyLitres.addAndGet(-amount);
    }
    
    public int getOxygenLevel() {
        return oxygenSupplyLitres.get();
    }

    public void refillOxygen(int amount) {
        oxygenSupplyLitres.addAndGet(amount);
    }

    public boolean isGeneratorActive() {
        return generatorActive.get();
    }

    public void activateGenerator() {
        generatorActive.set(true);
    }

    @Override
    public String toString() {
        return "Facility[" + hospitalId + ", O2=" + oxygenSupplyLitres.get() + "L, Gen=" + generatorActive.get() + "]";
    }
}
