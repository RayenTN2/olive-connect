package ISIMS.olive_connect.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class CreateCollectionTourRequest {

    @NotBlank(message = "Tour title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotBlank(message = "Team leader ID is required")
    private String teamLeaderId;

    @NotNull(message = "Planned start time is required")
    private Instant plannedStartTime;

    // Ordered harvest request IDs (defines initial orchard visit order 1..N)
    private List<String> harvestRequestIds;

    @Valid
    private List<OilMillDropOffInput> dropOffs;

    private List<String> workerIds;

    private List<String> equipmentIds;

    public static class OilMillDropOffInput {
        @NotBlank(message = "Oil mill ID is required")
        private String oilMillId;

        @Positive(message = "Allocated quantity must be greater than 0")
        private double allocatedQuantityKg;

        public String getOilMillId() { return oilMillId; }
        public void setOilMillId(String oilMillId) { this.oilMillId = oilMillId; }

        public double getAllocatedQuantityKg() { return allocatedQuantityKg; }
        public void setAllocatedQuantityKg(double allocatedQuantityKg) { this.allocatedQuantityKg = allocatedQuantityKg; }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTeamLeaderId() { return teamLeaderId; }
    public void setTeamLeaderId(String teamLeaderId) { this.teamLeaderId = teamLeaderId; }

    public Instant getPlannedStartTime() { return plannedStartTime; }
    public void setPlannedStartTime(Instant plannedStartTime) { this.plannedStartTime = plannedStartTime; }

    public List<String> getHarvestRequestIds() { return harvestRequestIds; }
    public void setHarvestRequestIds(List<String> harvestRequestIds) { this.harvestRequestIds = harvestRequestIds; }

    public List<OilMillDropOffInput> getDropOffs() { return dropOffs; }
    public void setDropOffs(List<OilMillDropOffInput> dropOffs) { this.dropOffs = dropOffs; }

    public List<String> getWorkerIds() { return workerIds; }
    public void setWorkerIds(List<String> workerIds) { this.workerIds = workerIds; }

    public List<String> getEquipmentIds() { return equipmentIds; }
    public void setEquipmentIds(List<String> equipmentIds) { this.equipmentIds = equipmentIds; }
}