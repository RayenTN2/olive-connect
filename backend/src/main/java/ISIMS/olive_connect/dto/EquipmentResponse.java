package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.Equipment;

import java.time.Instant;

public class EquipmentResponse {

    private String id;
    private String name;
    private String reference;
    private String type;
    private String brand;
    private String model;
    private String serialNumber;
    private String registrationNumber;
    private Integer capacityKg;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static EquipmentResponse fromEntity(Equipment equipment) {
        EquipmentResponse dto = new EquipmentResponse();
        dto.setId(equipment.getId());
        dto.setName(equipment.getName());
        dto.setReference(equipment.getReference());
        dto.setType(equipment.getType());
        dto.setBrand(equipment.getBrand());
        dto.setModel(equipment.getModel());
        dto.setSerialNumber(equipment.getSerialNumber());
        dto.setRegistrationNumber(equipment.getRegistrationNumber());
        dto.setCapacityKg(equipment.getCapacityKg());
        dto.setActive(equipment.isActive());
        dto.setCreatedAt(equipment.getCreatedAt());
        dto.setUpdatedAt(equipment.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
