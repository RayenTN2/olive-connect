package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.EquipmentUnavailabilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// US-25: Used for both creating and editing an equipment unavailability period
public class EquipmentUnavailabilityRequest {

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @NotNull(message = "Status is required")
    private EquipmentUnavailabilityStatus status;

    @NotBlank(message = "Reason is required")
    private String reason;

    // Getters and Setters
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public EquipmentUnavailabilityStatus getStatus() { return status; }
    public void setStatus(EquipmentUnavailabilityStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
