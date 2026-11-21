package ISIMS.olive_connect.service.routing;

import ISIMS.olive_connect.entity.Equipment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HarvestingDurationEstimator {

    private static final double BASE_KG_PER_WORKER_PER_HOUR = 25.0;
    private static final long SETUP_BUFFER_MINUTES = 15L;

    public long estimateMinutes(double quantityKg, int workerCount, List<Equipment> assignedEquipment) {
        if (quantityKg <= 0.0) {
            return 0L;
        }

        int effectiveWorkers = workerCount > 0 ? workerCount : 2;
        double effectiveKgPerHour = effectiveWorkers * BASE_KG_PER_WORKER_PER_HOUR;
        double hoursNeeded = quantityKg / Math.max(effectiveKgPerHour, 1.0);
        long rawMinutes = Math.round(hoursNeeded * 60.0);

        return Math.max(15L, rawMinutes + SETUP_BUFFER_MINUTES);
    }
}