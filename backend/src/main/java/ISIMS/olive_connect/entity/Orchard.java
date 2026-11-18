package ISIMS.olive_connect.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;
import java.util.List;

@Document(collection = "orchards")
public class Orchard {

    @Id
    private String id;

    @Indexed
    private String farmerId;

    private String name;

    private double surfaceAreaHectares;

    private int treeCount;

    private List<String> oliveVarieties;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location; // Stores [longitude, latitude]

    @Indexed
    private MaturityLevel currentMaturity = MaturityLevel.GREEN;

    @Indexed
    private OrchardHarvestStatus harvestStatus = OrchardHarvestStatus.IDLE;

    private Instant createdAt;
    private Instant updatedAt;

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

    public GeoJsonPoint getLocation() { return location; }
    public void setLocation(GeoJsonPoint location) { this.location = location; }

    public MaturityLevel getCurrentMaturity() { return currentMaturity; }
    public void setCurrentMaturity(MaturityLevel currentMaturity) { this.currentMaturity = currentMaturity; }

    public OrchardHarvestStatus getHarvestStatus() { return harvestStatus; }
    public void setHarvestStatus(OrchardHarvestStatus harvestStatus) { this.harvestStatus = harvestStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}