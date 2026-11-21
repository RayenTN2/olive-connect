package ISIMS.olive_connect.dto;

import java.time.Instant;
import java.util.List;

public class AvailableTourResourcesResponse {

    private String tourId;
    private Instant windowStart;
    private Instant windowEnd;
    private List<WorkerResponse> workers;
    private List<EquipmentResponse> equipment;

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }

    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }

    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }

    public List<WorkerResponse> getWorkers() { return workers; }
    public void setWorkers(List<WorkerResponse> workers) { this.workers = workers; }

    public List<EquipmentResponse> getEquipment() { return equipment; }
    public void setEquipment(List<EquipmentResponse> equipment) { this.equipment = equipment; }
}