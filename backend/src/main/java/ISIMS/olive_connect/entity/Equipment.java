package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// US-24: Harvesting/transport equipment managed by the Logistics Manager.
// Equipment is never deleted (only deactivated) so historical tours can still reference it by id.
@Document(collection = "equipment")
public class Equipment {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String reference;

    // Code of a built-in EquipmentType (e.g. "BIN") or of a CustomEquipmentType
    @Indexed
    private String type;

    // Optional, all types [TEAM]
    private String brand;
    private String model;
    private String serialNumber;

    // TRACTOR only, optional [TEAM]
    private String registrationNumber;

    // BIN only, required: needed by the F5 bin fill-rate KPI
    private Integer capacityKg;

    @Indexed
    private boolean active = true;

    private Instant createdAt;
    private Instant updatedAt;

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
