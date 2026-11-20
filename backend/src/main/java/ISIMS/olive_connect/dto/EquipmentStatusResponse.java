package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.EquipmentStatus;

// US-25: Current (computed) status of one equipment item
public class EquipmentStatusResponse {

    private String equipmentId;
    private EquipmentStatus status;
    private boolean active;

    public EquipmentStatusResponse() {}

    public EquipmentStatusResponse(String equipmentId, EquipmentStatus status, boolean active) {
        this.equipmentId = equipmentId;
        this.status = status;
        this.active = active;
    }

    // Getters and Setters
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
