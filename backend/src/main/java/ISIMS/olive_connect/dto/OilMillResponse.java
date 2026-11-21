package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.OilMill;

import java.time.Instant;

public class OilMillResponse {

    private String id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private double dailyCapacityKg;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static OilMillResponse fromEntity(OilMill mill) {
        OilMillResponse res = new OilMillResponse();
        res.setId(mill.getId());
        res.setName(mill.getName());
        res.setAddress(mill.getAddress());
        res.setLatitude(mill.getLatitude());
        res.setLongitude(mill.getLongitude());
        res.setDailyCapacityKg(mill.getDailyCapacityKg());
        res.setActive(mill.isActive());
        res.setCreatedAt(mill.getCreatedAt());
        res.setUpdatedAt(mill.getUpdatedAt());
        return res;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public double getDailyCapacityKg() { return dailyCapacityKg; }
    public void setDailyCapacityKg(double dailyCapacityKg) { this.dailyCapacityKg = dailyCapacityKg; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}