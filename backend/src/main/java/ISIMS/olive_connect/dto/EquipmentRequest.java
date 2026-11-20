package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// US-24: Used for both registering and updating equipment (PUT replaces all fields)
public class EquipmentRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Reference is required")
    private String reference;

    // Built-in or custom type code, checked by EquipmentTypeService
    @NotBlank(message = "Type is required")
    private String type;

    private String brand;
    private String model;
    private String serialNumber;
    private String registrationNumber;

    @Positive(message = "Capacity must be a positive number of kg")
    private Integer capacityKg;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public Integer getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Integer capacityKg) { this.capacityKg = capacityKg; }
}
