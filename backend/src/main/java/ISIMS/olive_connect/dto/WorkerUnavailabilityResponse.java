package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.WorkerUnavailability;

import java.time.Instant;

public class WorkerUnavailabilityResponse {

    private String id;
    private String workerId;
    private Instant startTime;
    private Instant endTime;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;

    public static WorkerUnavailabilityResponse fromEntity(WorkerUnavailability unavailability) {
        WorkerUnavailabilityResponse dto = new WorkerUnavailabilityResponse();
        dto.setId(unavailability.getId());
        dto.setWorkerId(unavailability.getWorkerId());
        dto.setStartTime(unavailability.getStartTime());
        dto.setEndTime(unavailability.getEndTime());
        dto.setReason(unavailability.getReason());
        dto.setCreatedAt(unavailability.getCreatedAt());
        dto.setUpdatedAt(unavailability.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
