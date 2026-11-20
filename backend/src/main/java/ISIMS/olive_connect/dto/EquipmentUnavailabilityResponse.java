package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.EquipmentUnavailability;
import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;

import java.time.Instant;

public class EquipmentUnavailabilityResponse {

    private String id;
    private String equipmentId;
    private Instant startTime;
    private Instant endTime;
    private EquipmentUnavailabilityStatus status;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;

    public static EquipmentUnavailabilityResponse fromEntity(EquipmentUnavailability unavailability) {
        EquipmentUnavailabilityResponse dto = new EquipmentUnavailabilityResponse();
        dto.setId(unavailability.getId());
        dto.setEquipmentId(unavailability.getEquipmentId());
        dto.setStartTime(unavailability.getStartTime());
        dto.setEndTime(unavailability.getEndTime());
        dto.setStatus(unavailability.getStatus());
        dto.setReason(unavailability.getReason());
        dto.setCreatedAt(unavailability.getCreatedAt());
        dto.setUpdatedAt(unavailability.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public EquipmentUnavailabilityStatus getStatus() { return status; }
    public void setStatus(EquipmentUnavailabilityStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
