package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;

import java.time.Instant;
import java.util.List;

public class OrchardResponse {

    private String id;
    private String farmerId;
    private String name;
    private double surfaceAreaHectares;
    private int treeCount;
    private List<String> oliveVarieties;
    private double longitude;
    private double latitude;
    private MaturityLevel currentMaturity;
    private OrchardHarvestStatus harvestStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrchardResponse fromEntity(Orchard orchard) {
        OrchardResponse res = new OrchardResponse();
        res.setId(orchard.getId());
        res.setFarmerId(orchard.getFarmerId());
        res.setName(orchard.getName());
        res.setSurfaceAreaHectares(orchard.getSurfaceAreaHectares());
        res.setTreeCount(orchard.getTreeCount());
        res.setOliveVarieties(orchard.getOliveVarieties());
        if (orchard.getLocation() != null && orchard.getLocation().getCoordinates() != null) {
            res.setLongitude(orchard.getLocation().getX()); // X = longitude
            res.setLatitude(orchard.getLocation().getY());  // Y = latitude
        }
        res.setCurrentMaturity(orchard.getCurrentMaturity());
        res.setHarvestStatus(orchard.getHarvestStatus());
        res.setCreatedAt(orchard.getCreatedAt());
        res.setUpdatedAt(orchard.getUpdatedAt());
        return res;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getSurfaceAreaHectares() { return surfaceAreaHectares; }
    public void setSurfaceAreaHectares(double surfaceAreaHectares) { this.surfaceAreaHectares = surfaceAreaHectares; }

    public int getTreeCount() { return treeCount; }
    public void setTreeCount(int treeCount) { this.treeCount = treeCount; }

    public List<String> getOliveVarieties() { return oliveVarieties; }
    public void setOliveVarieties(List<String> oliveVarieties) { this.oliveVarieties = oliveVarieties; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public MaturityLevel getCurrentMaturity() { return currentMaturity; }
    public void setCurrentMaturity(MaturityLevel currentMaturity) { this.currentMaturity = currentMaturity; }

    public OrchardHarvestStatus getHarvestStatus() { return harvestStatus; }
    public void setHarvestStatus(OrchardHarvestStatus harvestStatus) { this.harvestStatus = harvestStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}