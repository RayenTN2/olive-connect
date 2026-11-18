package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class RegisterOrchardRequest {

    @NotBlank(message = "Orchard name is required")
    private String name;

    @Positive(message = "Surface area must be greater than 0")
    private double surfaceAreaHectares;

    @Min(value = 1, message = "Tree count must be at least 1")
    private int treeCount;

    @NotEmpty(message = "At least one olive variety must be specified")
    private List<String> oliveVarieties;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getSurfaceAreaHectares() { return surfaceAreaHectares; }
    public void setSurfaceAreaHectares(double surfaceAreaHectares) { this.surfaceAreaHectares = surfaceAreaHectares; }

    public int getTreeCount() { return treeCount; }
    public void setTreeCount(int treeCount) { this.treeCount = treeCount; }

    public List<String> getOliveVarieties() { return oliveVarieties; }
    public void setOliveVarieties(List<String> oliveVarieties) { this.oliveVarieties = oliveVarieties; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
}