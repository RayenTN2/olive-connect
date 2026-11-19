package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// US-23: Used for both creating and editing a worker unavailability interval
public class WorkerUnavailabilityRequest {

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @NotBlank(message = "Reason is required")
    private String reason;

    // Getters and Setters
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
