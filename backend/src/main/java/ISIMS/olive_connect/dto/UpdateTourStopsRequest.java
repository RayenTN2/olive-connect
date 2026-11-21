package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UpdateTourStopsRequest {

    @NotEmpty(message = "At least one harvest request ID is required")
    private List<String> orderedHarvestRequestIds;

    public List<String> getOrderedHarvestRequestIds() { return orderedHarvestRequestIds; }
    public void setOrderedHarvestRequestIds(List<String> orderedHarvestRequestIds) {
        this.orderedHarvestRequestIds = orderedHarvestRequestIds;
    }
}