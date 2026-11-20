package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotNull;

// US-24: Activate or deactivate equipment
public class UpdateEquipmentStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
