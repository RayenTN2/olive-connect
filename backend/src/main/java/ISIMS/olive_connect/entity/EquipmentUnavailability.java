package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// US-25: A period [startTime, endTime) during which equipment is under maintenance or unavailable.
// Same model as WorkerUnavailability (US-23): own collection, equipment referenced by id.
@Document(collection = "equipment_unavailabilities")
public class EquipmentUnavailability {

    @Id
    private String id;

    @Indexed
    private String equipmentId;

    @Indexed
    private Instant startTime;

    private Instant endTime;
    private EquipmentUnavailabilityStatus status;
    private String reason;

    private Instant createdAt;
    private Instant updatedAt;

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
