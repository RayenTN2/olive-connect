package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// US-23: A period [startTime, endTime) during which a worker cannot be assigned (e.g. leave, absence).
// Stored in its own collection (referencing the worker by id) so overlap queries run across all workers.
@Document(collection = "worker_unavailabilities")
public class WorkerUnavailability {

    @Id
    private String id;

    @Indexed
    private String workerId;

    @Indexed
    private Instant startTime;

    private Instant endTime;
    private String reason;

    private Instant createdAt;
    private Instant updatedAt;

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
