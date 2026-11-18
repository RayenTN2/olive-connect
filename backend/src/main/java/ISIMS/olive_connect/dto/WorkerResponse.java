package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.Worker;

import java.time.Instant;

public class WorkerResponse {

    private String id;
    private String fullName;
    private String phone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static WorkerResponse fromEntity(Worker worker) {
        WorkerResponse dto = new WorkerResponse();
        dto.setId(worker.getId());
        dto.setFullName(worker.getFullName());
        dto.setPhone(worker.getPhone());
        dto.setActive(worker.isActive());
        dto.setCreatedAt(worker.getCreatedAt());
        dto.setUpdatedAt(worker.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
