package com.medgrid.model;

import java.util.Arrays;
import java.util.List;

public enum CaseType {
    CARDIAC(Medicine.ASPIRIN, Medicine.BLOOD_UNIT),
    TRAUMA(Medicine.MORPHINE, Medicine.BLOOD_UNIT, Medicine.ANESTHESIA),
    PEDIATRIC(Medicine.ANTIBIOTICS, Medicine.PAINKILLERS),
    BURN(Medicine.MORPHINE, Medicine.ANTIBIOTICS),
    NEUROLOGICAL(Medicine.ANESTHESIA),
    GENERAL(Medicine.PAINKILLERS, Medicine.ANTIBIOTICS),
    DENTAL(Medicine.PAINKILLERS, Medicine.ANESTHESIA),
    OPHTHALMOLOGY(Medicine.EYE_DROPS, Medicine.ANTIBIOTICS);

    private final List<Medicine> requiredMedicines;

    CaseType(Medicine... medicines) {
        this.requiredMedicines = Arrays.asList(medicines);
    }

    public List<Medicine> getRequiredMedicines() {
        return requiredMedicines;
    }
}
