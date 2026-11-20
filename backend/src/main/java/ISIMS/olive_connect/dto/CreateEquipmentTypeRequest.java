package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotBlank;

// US-24: Add a custom equipment type; its code is generated from the label
public class CreateEquipmentTypeRequest {

    @NotBlank(message = "Label is required")
    private String label;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
