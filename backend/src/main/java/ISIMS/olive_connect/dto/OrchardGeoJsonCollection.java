package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.MaturityLevel;
import ISIMS.olive_connect.entity.Orchard;
import ISIMS.olive_connect.entity.OrchardHarvestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public class OrchardGeoJsonCollection {

    private final String type = "FeatureCollection";
    private List<OrchardFeature> features;

    public OrchardGeoJsonCollection(List<OrchardFeature> features) {
        this.features = features;
    }

    public String getType() { return type; }
    public List<OrchardFeature> getFeatures() { return features; }
    public void setFeatures(List<OrchardFeature> features) { this.features = features; }

    public static class OrchardFeature {
        private final String type = "Feature";
        private String id;
        private Geometry geometry;
        private Properties properties;

        public static OrchardFeature fromEntity(Orchard orchard, boolean isPublicView) {
            OrchardFeature feature = new OrchardFeature();
            feature.setId(orchard.getId());

            if (orchard.getLocation() != null) {
                feature.setGeometry(new Geometry(
                        orchard.getLocation().getX(), // longitude
                        orchard.getLocation().getY()  // latitude
                ));
            }

            Properties props = new Properties();
            props.setOrchardId(orchard.getId());
            // Hide farmerId for unauthenticated public visitors to protect farmer privacy
            if (!isPublicView) {
                props.setFarmerId(orchard.getFarmerId());
            }
            props.setName(orchard.getName());
            props.setSurfaceAreaHectares(orchard.getSurfaceAreaHectares());
            props.setTreeCount(orchard.getTreeCount());
            props.setOliveVarieties(orchard.getOliveVarieties());
            props.setCurrentMaturity(orchard.getCurrentMaturity());
            props.setHarvestStatus(orchard.getHarvestStatus());
            props.setUpdatedAt(orchard.getUpdatedAt());
            feature.setProperties(props);

            return feature;
        }

        public String getType() { return type; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Geometry getGeometry() { return geometry; }
        public void setGeometry(Geometry geometry) { this.geometry = geometry; }
        public Properties getProperties() { return properties; }
        public void setProperties(Properties properties) { this.properties = properties; }
    }

    public static class Geometry {
        private final String type = "Point";
        private List<Double> coordinates; // [longitude, latitude]

        public Geometry(double longitude, double latitude) {
            this.coordinates = List.of(longitude, latitude);
        }

        public String getType() { return type; }
        public List<Double> getCoordinates() { return coordinates; }
        public void setCoordinates(List<Double> coordinates) { this.coordinates = coordinates; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Properties {
        private String orchardId;
        private String farmerId;
        private String name;
        private double surfaceAreaHectares;
        private int treeCount;
        private List<String> oliveVarieties;
        private MaturityLevel currentMaturity;
        private OrchardHarvestStatus harvestStatus;
        private Instant updatedAt;

        public String getOrchardId() { return orchardId; }
        public void setOrchardId(String orchardId) { this.orchardId = orchardId; }
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
        public MaturityLevel getCurrentMaturity() { return currentMaturity; }
        public void setCurrentMaturity(MaturityLevel currentMaturity) { this.currentMaturity = currentMaturity; }
        public OrchardHarvestStatus getHarvestStatus() { return harvestStatus; }
        public void setHarvestStatus(OrchardHarvestStatus harvestStatus) { this.harvestStatus = harvestStatus; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}